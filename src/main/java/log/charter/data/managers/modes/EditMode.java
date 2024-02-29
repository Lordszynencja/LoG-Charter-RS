package log.charter.data.managers.modes;

public enum EditMode {
	EMPTY("Empty"), //
	GUITAR("Guitar"), //
	TEMPO_MAP("Tempo map"), //
	VOCALS("Vocals");

	public final String label;

	private EditMode(final String label) {
		this.label = label;
	}
}
