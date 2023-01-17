package log.charter.data.managers.modes;

import static log.charter.gui.chartPanelDrawers.common.DrawerUtils.yToLane;
import static log.charter.song.notes.IPosition.findClosestId;
import static log.charter.song.notes.IPositionWithLength.changePositionsWithLengthsLength;

import log.charter.data.ChartData;
import log.charter.data.config.Config;
import log.charter.data.managers.HighlightManager;
import log.charter.data.managers.PositionWithStringOrNoteId;
import log.charter.data.managers.selection.SelectionAccessor;
import log.charter.data.managers.selection.SelectionManager;
import log.charter.data.types.PositionType;
import log.charter.data.types.PositionWithIdAndType;
import log.charter.data.undoSystem.UndoSystem;
import log.charter.gui.CharterFrame;
import log.charter.gui.handlers.KeyboardHandler;
import log.charter.gui.handlers.MouseButtonPressReleaseHandler.MouseButtonPressReleaseData;
import log.charter.gui.panes.AnchorPane;
import log.charter.gui.panes.ChordOptionsPane;
import log.charter.gui.panes.GuitarBeatPane;
import log.charter.gui.panes.HandShapePane;
import log.charter.gui.panes.ToneChangePane;
import log.charter.song.Anchor;
import log.charter.song.HandShape;
import log.charter.song.ToneChange;
import log.charter.song.notes.ChordOrNote;
import log.charter.song.notes.GuitarSound;
import log.charter.song.notes.IPosition;
import log.charter.song.notes.IPositionWithLength;
import log.charter.song.notes.Note;
import log.charter.util.CollectionUtils.ArrayList2;

public class GuitarModeHandler extends ModeHandler {
	private ChartData data;
	private CharterFrame frame;
	private HighlightManager highlightManager;
	private KeyboardHandler keyboardHandler;
	SelectionManager selectionManager;
	private UndoSystem undoSystem;

	public void init(final ChartData data, final CharterFrame frame, final HighlightManager highlightManager,
			final KeyboardHandler keyboardHandler, final SelectionManager selectionManager,
			final UndoSystem undoSystem) {
		this.data = data;
		this.frame = frame;
		this.highlightManager = highlightManager;
		this.keyboardHandler = keyboardHandler;
		this.selectionManager = selectionManager;
		this.undoSystem = undoSystem;
	}

	@Override
	public void handleEnd() {
		final ArrayList2<ChordOrNote> sounds = data.getCurrentArrangementLevel().chordsAndNotes;
		if (!sounds.isEmpty()) {
			frame.setNextTime(sounds.getLast().position());
		}
	}

	@Override
	public void handleHome() {
		final ArrayList2<ChordOrNote> sounds = data.getCurrentArrangementLevel().chordsAndNotes;
		if (!sounds.isEmpty()) {
			frame.setNextTime(sounds.get(0).position());
		}
	}

	@Override
	public void snapNotes() {// TODO
		undoSystem.addUndo();

		selectionManager.getSelectedAccessor(PositionType.GUITAR_NOTE);
		selectionManager.getSelectedAccessor(PositionType.HAND_SHAPE);
	}

	private void rightClickAnchor(final PositionWithIdAndType anchorPosition) {
		selectionManager.clear();

		if (anchorPosition.anchor != null) {
			new AnchorPane(data, frame, undoSystem, anchorPosition.anchor, () -> {
			});
			return;
		}

		undoSystem.addUndo();
		final Anchor anchor = new Anchor(anchorPosition.position(), 0);
		final ArrayList2<Anchor> anchors = data.getCurrentArrangementLevel().anchors;
		anchors.add(anchor);
		anchors.sort(null);

		new AnchorPane(data, frame, undoSystem, anchor, () -> {
			undoSystem.undo();
			undoSystem.removeRedo();
		});
	}

	private void rightClickBeat(final PositionWithIdAndType beatPosition) {
		if (beatPosition.beat == null) {
			return;
		}

		new GuitarBeatPane(data, frame, undoSystem, beatPosition.beat);
	}

	private void rightClickHandShape(final PositionWithIdAndType handShapePosition) {
		selectionManager.clear();
		if (handShapePosition.handShape != null) {
			data.getCurrentArrangementLevel().handShapes.remove((int) handShapePosition.id);
			return;
		}

		undoSystem.addUndo();

		final int endPosition = data.songChart.beatsMap.getNextPositionFromGridAfter(handShapePosition.position());

		final HandShape handShape = new HandShape(handShapePosition.position(),
				endPosition - handShapePosition.position());
		final ArrayList2<HandShape> handShapes = data.getCurrentArrangementLevel().handShapes;
		handShapes.add(handShape);
		handShapes.sort(null);

		new HandShapePane(data, frame, handShape, () -> {
			undoSystem.undo();
			undoSystem.removeRedo();
		});
	}

	private ChordOrNote addSound(final Note note) {
		final ArrayList2<ChordOrNote> sounds = data.getCurrentArrangementLevel().chordsAndNotes;
		final ChordOrNote sound = new ChordOrNote(note);
		sounds.add(new ChordOrNote(note));
		sounds.sort(null);

		final ChordOrNote previous = IPosition.findLastBefore(sounds, note.position());
		if (previous != null) {
			final GuitarSound previousSound = previous.asGuitarSound();
			if (previousSound.linkNext) {
				if (previousSound.endPosition() > note.position()) {
					previousSound.length(note.position() - previousSound.position());
				}
			} else if (previousSound.endPosition() > note.position() - Config.minNoteDistance) {
				previousSound.length(note.position() - Config.minNoteDistance - previousSound.position());
			}
		}

		selectionManager.addSoundSelection(findClosestId(sounds, note));

		return sound;
	}

	private void addSingleNote(final MouseButtonPressReleaseData clickData) {
		final ArrayList2<ChordOrNote> sounds = data.getCurrentArrangementLevel().chordsAndNotes;
		final int string = yToLane(clickData.pressPosition.y, data.currentStrings());
		if (string < 0 || string >= data.currentStrings()) {
			return;
		}

		if (clickData.pressHighlight.chordOrNote != null) {
			final ChordOrNote chordOrNote = clickData.pressHighlight.chordOrNote;
			if (chordOrNote.isChord() || chordOrNote.note.string != string) {
				selectionManager.addSoundSelection(clickData.pressHighlight.id);
				new ChordOptionsPane(data, frame, undoSystem, new ArrayList2<>(chordOrNote));
				return;
			}

			sounds.remove(clickData.pressHighlight.chordOrNote);
			return;
		}

		final Note note = new Note(clickData.pressHighlight.position(), string, 0);
		addSound(note);
	}

	private void addMultipleNotes(final MouseButtonPressReleaseData clickData) {
		final ArrayList2<ChordOrNote> sounds = data.getCurrentArrangementLevel().chordsAndNotes;
		final ArrayList2<PositionWithStringOrNoteId> positions = highlightManager.getPositionsWithStrings(
				clickData.pressHighlight.position(), clickData.releaseHighlight.position(), clickData.pressPosition.y,
				clickData.releasePosition.y);

		final ArrayList2<ChordOrNote> editedSounds = new ArrayList2<>();

		int insertedNotes = 0;
		for (final PositionWithStringOrNoteId position : positions) {
			if (position.noteId != null) {
				final int noteId = position.noteId + insertedNotes;
				editedSounds.add(sounds.get(noteId));
				selectionManager.addSoundSelection(noteId);
			} else {
				final Note note = new Note(position.position(), position.string, 0);
				final ChordOrNote sound = addSound(note);
				insertedNotes++;
				editedSounds.add(sound);
			}
		}

		if (editedSounds.get(0).isChord() || editedSounds.get(0).note.string != positions.get(0).string) {
			new ChordOptionsPane(data, frame, undoSystem, editedSounds);
		}
	}

	private void rightClickGuitarNote(final MouseButtonPressReleaseData clickData) {
		selectionManager.clear();
		undoSystem.addUndo();

		if (!clickData.isDrag()) {
			addSingleNote(clickData);
			return;
		}

		addMultipleNotes(clickData);
	}

	private void rightClickToneChange(final PositionWithIdAndType toneChangePosition) {
		selectionManager.clear();

		if (toneChangePosition.toneChange != null) {
			new ToneChangePane(data, frame, undoSystem, toneChangePosition.toneChange, () -> {
			});
			return;
		}

		undoSystem.addUndo();
		final ToneChange toneChange = new ToneChange(toneChangePosition.position(), "");
		final ArrayList2<ToneChange> toneChanges = data.getCurrentArrangement().toneChanges;
		toneChanges.add(toneChange);
		toneChanges.sort(null);

		new ToneChangePane(data, frame, undoSystem, toneChange, () -> {
			undoSystem.undo();
			undoSystem.removeRedo();
		});
	}

	@Override
	public void rightClick(final MouseButtonPressReleaseData clickData) {
		if (clickData.pressHighlight.type == PositionType.ANCHOR) {
			if (clickData.isDrag()) {
				return;
			}

			rightClickAnchor(clickData.pressHighlight);
			return;
		}

		if (clickData.pressHighlight.type == PositionType.BEAT) {
			if (clickData.isDrag()) {
				return;
			}

			rightClickBeat(clickData.pressHighlight);
			return;
		}

		if (clickData.pressHighlight.type == PositionType.GUITAR_NOTE) {
			rightClickGuitarNote(clickData);
			return;
		}

		if (clickData.pressHighlight.type == PositionType.HAND_SHAPE) {
			if (clickData.isDrag()) {
				return;
			}

			rightClickHandShape(clickData.pressHighlight);
			return;
		}

		if (clickData.pressHighlight.type == PositionType.TONE_CHANGE) {
			if (clickData.isDrag()) {
				return;
			}

			rightClickToneChange(clickData.pressHighlight);
			return;
		}
	}

	private void changeNotesLength(final int change) {
		final SelectionAccessor<ChordOrNote> selectedNotes = selectionManager
				.getSelectedAccessor(PositionType.GUITAR_NOTE);
		IPositionWithLength.changeNotesLength(data.songChart.beatsMap, selectedNotes.getSortedSelected(),
				data.getCurrentArrangementLevel().chordsAndNotes, change, !keyboardHandler.alt());
	}

	private void changeHandShapesLength(final int change) {
		final SelectionAccessor<HandShape> selectedNotes = selectionManager
				.getSelectedAccessor(PositionType.HAND_SHAPE);
		changePositionsWithLengthsLength(data.songChart.beatsMap, selectedNotes.getSortedSelected(),
				data.getCurrentArrangementLevel().handShapes, change);
	}

	@Override
	public void changeLength(int change) {
		if (keyboardHandler.shift()) {
			change *= 4;
		}

		changeNotesLength(change);
		changeHandShapesLength(change);
	}
}
