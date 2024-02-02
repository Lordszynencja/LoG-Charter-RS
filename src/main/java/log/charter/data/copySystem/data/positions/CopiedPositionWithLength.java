package log.charter.data.copySystem.data.positions;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import log.charter.song.BeatsMap;
import log.charter.song.notes.IPositionWithLength;

public abstract class CopiedPositionWithLength<T extends IPositionWithLength> extends CopiedPosition<T> {
	@XStreamAsAttribute
	public final int length;
	@XStreamAsAttribute
	public final double beatsLength;

	public CopiedPositionWithLength(final BeatsMap beatsMap, final int basePosition, final double basePositionInBeats,
			final T positionWithLength) {
		super(beatsMap, basePosition, basePositionInBeats, positionWithLength);
		length = positionWithLength.length();
		beatsLength = beatsMap.getPositionInBeats(positionWithLength.endPosition()) - positionInBeats
				- basePositionInBeats;
	}

	@Override
	protected abstract T prepareValue();

	@Override
	public T getValue(final BeatsMap beatsMap, final int basePosition, final double basePositionInBeats,
			final boolean convertFromBeats) {
		final T value = super.getValue(beatsMap, basePosition, basePositionInBeats, convertFromBeats);
		if (value == null) {
			return null;
		}

		if (convertFromBeats) {
			double endPositionInBeats = basePositionInBeats + positionInBeats + beatsLength;
			if (endPositionInBeats > beatsMap.beats.size() - 1) {
				endPositionInBeats = beatsMap.beats.size() - 1;
			}

			value.length(beatsMap.getPositionForPositionInBeats(endPositionInBeats) - value.position());
		} else {
			value.length(length);
		}

		return value;
	}
}
