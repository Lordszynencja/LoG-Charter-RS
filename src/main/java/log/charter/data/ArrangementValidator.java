package log.charter.data;

import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import log.charter.data.config.Localization.Label;
import log.charter.data.managers.ModeManager;
import log.charter.gui.CharterFrame;
import log.charter.gui.handlers.data.ChartTimeHandler;
import log.charter.song.Arrangement;
import log.charter.song.EventPoint;
import log.charter.util.CollectionUtils.ArrayList2;

public class ArrangementValidator {
	private ChartTimeHandler chartTimeHandler;
	private ChartData data;
	private CharterFrame frame;
	private ModeManager modeManager;

	public void init(final ChartTimeHandler chartTimeHandler, final ChartData data, final CharterFrame frame,
			final ModeManager modeManager) {
		this.chartTimeHandler = chartTimeHandler;
		this.data = data;
		this.frame = frame;
		this.modeManager = modeManager;
	}

	private Runnable moveToTimeOnArrangement(final int arrangementId, final int time) {
		return () -> {
			modeManager.setArrangement(arrangementId);
			chartTimeHandler.setNextTime(time);

			frame.updateEditAreaSizes();
		};
	}

	/**
	 * @return true if validation should continue
	 */
	private boolean showWarning(final Label msg, final Runnable onYes) {
		final int result = JOptionPane.showConfirmDialog(frame, msg.label(), "", JOptionPane.YES_NO_CANCEL_OPTION);

		if (result == JOptionPane.YES_OPTION) {
			onYes.run();
			return false;
		}
		if (result == JOptionPane.NO_OPTION) {
			return true;
		}
		if (result == JOptionPane.CANCEL_OPTION) {
			return false;
		}

		return true;
	}

	private boolean validateCountPhrases(final int arrangementId, final Arrangement arrangement) {
		final ArrayList2<EventPoint> countPhrases = arrangement.eventPoints.stream()//
				.filter(eventPoint -> eventPoint.phrase != null && eventPoint.phrase.equals("COUNT"))//
				.collect(Collectors.toCollection(ArrayList2::new));
		if (countPhrases.isEmpty()) {
			final boolean warningStoppedValidation = !showWarning(Label.COUNT_PHRASE_MISSING,
					moveToTimeOnArrangement(arrangementId, 0));
			if (warningStoppedValidation) {
				return false;
			}
		} else if (countPhrases.size() > 1) {
			final boolean warningStoppedValidation = !showWarning(Label.COUNT_PHRASE_MULTIPLE,
					moveToTimeOnArrangement(arrangementId, countPhrases.getLast().position()));
			if (warningStoppedValidation) {
				return false;
			}
		}

		return true;
	}

	private boolean validateEndPhrases(final int arrangementId, final Arrangement arrangement) {
		final ArrayList2<EventPoint> endPhrases = arrangement.eventPoints.stream()//
				.filter(eventPoint -> eventPoint.phrase != null && eventPoint.phrase.equals("END"))//
				.collect(Collectors.toCollection(ArrayList2::new));
		if (endPhrases.isEmpty()) {
			final boolean warningStoppedValidation = !showWarning(Label.END_PHRASE_MISSING,
					moveToTimeOnArrangement(arrangementId, data.songChart.beatsMap.beats.getLast().position()));
			if (warningStoppedValidation) {
				return false;
			}
		} else if (endPhrases.size() > 1) {
			final boolean warningStoppedValidation = !showWarning(Label.END_PHRASE_MULTIPLE,
					moveToTimeOnArrangement(arrangementId, endPhrases.get(0).position()));
			if (warningStoppedValidation) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @return true if validation passed
	 */
	public boolean validate() {
		final ArrayList2<Arrangement> arrangements = data.songChart.arrangements;
		for (int i = 0; i < arrangements.size(); i++) {
			final Arrangement arrangement = arrangements.get(i);
			if (!validateCountPhrases(i, arrangement)) {
				return false;
			}
			if (!validateEndPhrases(i, arrangement)) {
				return false;
			}
		}

		return true;
	}
}
