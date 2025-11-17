/*
*
* Copyright (C) 2025 Owen Forsyth and Daniel Mead
*
* This program is free software: you can redistribute it and/or modify 
* it under the terms of the GNU General Public License as published by 
* the Free Software Foundation, either version 3 of the License, or 
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful, 
* but WITHOUT ANY WARRANTY; without even the implied warranty of 
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
* General Public License for more details.
*
* You should have received a copy of the GNU General Public License 
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*
*/

package com.dynamicduo.proto.render;

import java.nio.file.Path;

/**
 * SvgRenderer
 * Runs the Graphviz 'dot' command to produce an SVG from a .dot file.
 * Requires Graphviz installed and on PATH. (macOS: brew install graphviz)
 */
public final class SvgRenderer {
    private SvgRenderer() {
    }

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
                    "-o", svgFile.toAbsolutePath().toString()).redirectErrorStream(true).start();

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
