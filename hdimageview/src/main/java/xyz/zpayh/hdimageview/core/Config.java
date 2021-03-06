package xyz.zpayh.hdimageview.core;

public enum Config {
    ALPHA_8     (1),

    /**
     * Each pixel is stored on 2 bytes and only the RGB channels are
     * encoded: red is stored with 5 bits of precision (32 possible
     * values), green is stored with 6 bits of precision (64 possible
     * values) and blue is stored with 5 bits of precision.
     *
     * This configuration can produce slight visual artifacts depending
     * on the configuration of the source. For instance, without
     * dithering, the result might show a greenish tint. To get better
     * results dithering should be applied.
     *
     * This configuration may be useful when using opaque bitmaps
     * that do not require high color fidelity.
     *
     * <p>Use this formula to pack into 16 bits:</p>
     * <pre class="prettyprint">
     * short color = (R & 0x1f) << 11 | (G & 0x3f) << 5 | (B & 0x1f);
     * </pre>
     */
    RGB_565     (3),

    /**
     * Each pixel is stored on 2 bytes. The three RGB color channels
     * and the alpha channel (translucency) are stored with a 4 bits
     * precision (16 possible values.)
     *
     * This configuration is mostly useful if the application needs
     * to store translucency information but also needs to save
     * memory.
     *
     * It is recommended to use {@link #ARGB_8888} instead of this
     * configuration.
     *
     * Note: as of
     * any bitmap created with this configuration will be created
     * using {@link #ARGB_8888} instead.
     *
     * @deprecated Because of the poor quality of this configuration,
     *             it is advised to use {@link #ARGB_8888} instead.
     */
    @Deprecated
    ARGB_4444   (4),

    /**
     * Each pixel is stored on 4 bytes. Each channel (RGB and alpha
     * for translucency) is stored with 8 bits of precision (256
     * possible values.)
     *
     * This configuration is very flexible and offers the best
     * quality. It should be used whenever possible.
     *
     * <p>Use this formula to pack into 32 bits:</p>
     * <pre class="prettyprint">
     * int color = (A & 0xff) << 24 | (B & 0xff) << 16 | (G & 0xff) << 8 | (R & 0xff);
     * </pre>
     */
    ARGB_8888   (5),

    /**
     * Each pixels is stored on 8 bytes. Each channel (RGB and alpha
     * for translucency) is stored as a
     * {@link  -precision floating point value}.
     *
     * This configuration is particularly suited for wide-gamut and
     * HDR content.
     *
     * <p>Use this formula to pack into 64 bits:</p>
     * <pre class="prettyprint">
     * long color = (A & 0xffff) << 48 | (B & 0xffff) << 32 | (G & 0xffff) << 16 | (R & 0xffff);
     * </pre>
     */
    RGBA_F16    (6),

    /**
     * Special configuration, when bitmap is stored only in graphic memory.
     * Bitmaps in this configuration are always immutable.
     *
     * It is optimal for cases, when the only operation with the bitmap is to draw it on a
     * screen.
     */
    HARDWARE    (7);

//    @UnsupportedAppUsage
    final int nativeInt;

    private static Config sConfigs[] = {
            null, ALPHA_8, null, RGB_565, ARGB_4444, ARGB_8888, RGBA_F16, HARDWARE
    };

    Config(int ni) {
        this.nativeInt = ni;
    }

//    @UnsupportedAppUsage
    static Config nativeToConfig(int ni) {
        return sConfigs[ni];
    }
}