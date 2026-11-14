package com.elephantjava.contex;

public final class VisitorContext {

    private static final ThreadLocal<String> SESSION = new ThreadLocal<>();

    private VisitorContext() {
    }

    public static void set(String sessionId) {
        SESSION.set(sessionId);
    }

    public static String get() {
        return SESSION.get();
    }

    public static void clear() {
        SESSION.remove();
    }
}
