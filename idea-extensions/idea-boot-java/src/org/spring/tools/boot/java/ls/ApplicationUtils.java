package org.spring.tools.boot.java.ls;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;

public final class ApplicationUtils {
    private ApplicationUtils() {
    }

    public static <T> T runReadAction(Computable<T> computable) {
        return ApplicationManager.getApplication().runReadAction(computable);
    }
}
