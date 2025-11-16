package com.dynamicduo.proto.render;

import java.nio.file.Path;

/**
 * SvgRenderer
 * Runs the Graphviz 'dot' command to produce an SVG from a .dot file.
 * Requires Graphviz installed and on PATH. (macOS: brew install graphviz)
 */
public final class SvgRenderer {
    private SvgRenderer() {}

    /**
     * @param dotFile path to the .dot file
     * @param svgFile desired output .svg path
     * @return true if SVG rendered successfully
     */
    public static boolean render(Path dotFile, Path svgFile) {
        try {
            Process p = new ProcessBuilder(
                    "dot", "-Tsvg",
                    dotFile.toAbsolutePath().toString(),
                    "-o", svgFile.toAbsolutePath().toString()
            ).redirectErrorStream(true).start();

            int code = p.waitFor();
            if (code == 0) {
                System.out.println("[SvgRenderer] Created SVG: " + svgFile.toAbsolutePath());
                return true;
            } else {
                System.err.println("[SvgRenderer] Graphviz exited with code " + code);
            }
        } catch (Exception e) {
            System.err.println("[SvgRenderer] Failed: " + e.getMessage());
        }
        return false;
    }
}
