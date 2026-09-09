package com.hotelos.util;

import com.github.f4b6a3.ulid.UlidCreator;

public final class PublicIds {

    private PublicIds() {
    }

    public static String next() {
        return UlidCreator.getUlid().toString();
    }
}
