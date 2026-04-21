package org.gensokyo.plugin.easydoc.kit;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public final class I18nKit {
    private static final String BUNDLE_NAME = "messages";
    private static Locale locale = Locale.SIMPLIFIED_CHINESE;
    private static ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);

    private I18nKit() {
        throw new UnsupportedOperationException();
    }

    public static void setLocale(Locale newLocale) {
        if (newLocale == null) {
            return;
        }
        locale = newLocale;
        bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
    }

    public static Locale getLocale() {
        return locale;
    }

    public static String t(String key, Object... args) {
        String value = bundle.containsKey(key) ? bundle.getString(key) : key;
        return args == null || args.length == 0 ? value : MessageFormat.format(value, args);
    }
}
