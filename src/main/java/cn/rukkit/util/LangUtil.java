package cn.rukkit.util;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class LangUtil {
	public static Locale lc = Locale.getDefault();
	private static ResourceBundle bundle;
	
    public static void setLocale(Locale locale) {
		lc = locale;
	}
	
	public static String getString(String text) {
		if (bundle == null || bundle.getLocale() != lc) {
			ResourceBundle bd = ResourceBundle.getBundle("i18n/messages", lc);
			bundle = bd;
		}
		return new String(bundle.getString(text).getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
	}

	public static String getFormatString(String text, Object ... format) {
		return MessageFormat.format(getString(text), format);
	}
}
