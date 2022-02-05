package cn.rukkit.util;
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
		return bundle.getString(text);
	}
}
