package com.l7cor1s.viktoria.platform;

import com.l7cor1s.viktoria.entity.VikaEntity;

/**
 * Hooks from common code into client-only code. The client entry point installs the real
 * implementation; on a dedicated server the no-op default stays in place, so no client class is
 * ever loaded there.
 *
 * @author vyrriox
 */
public interface ClientBridge {
    ClientBridge NOOP = new ClientBridge() {
    };

    /** Opens the interaction menu for the given Laura (client side only). */
    default void openInteractionScreen(VikaEntity laura) {
    }

    /** True when the local player is currently looking at a Vika menu or screen. */
    default boolean isScreenOpen() {
        return false;
    }

    /** A mod packet received from the server, already on the client thread. */
    default void handlePacket(byte[] data) {
    }
}
