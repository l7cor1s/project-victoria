package com.l7cor1s.viktoria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Identity of the mod for the code that does not depend on Minecraft or a loader: its id, its
 * display name and its logger. Shared by every target, so it never needs a port.
 *
 * @author vyrriox
 */
public final class ViktoriaCore {
    /** Registry / resource namespace. Kept as {@code viktoria} so existing worlds and packs stay valid. */
    public static final String MODID = "viktoria";
    /** On-disk config folder: {@code config/projectvika/}. */
    public static final String CONFIG_FOLDER = "projectvika";
    public static final String NAME = "Project Vika";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    private ViktoriaCore() {
    }
}
