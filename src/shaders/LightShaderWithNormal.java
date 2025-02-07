package shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public final class LightShaderWithNormal {
    public static ShaderProgram createLightShader() {
        // Shader adapted from https://github.com/mattdesl/lwjgl-basics/wiki/ShaderLesson6
        final String vertexShader =
                "#version 330 core\n" +
                        "attribute vec4 vertex_positions;\n" //
                        + "attribute vec4 quad_colors;\n" //
                        + "attribute float s;\n"
                        + "uniform mat4 u_projTrans;\n" //
                        + "varying vec4 v_color;\n" //
                        + "void main()\n" //
                        + "{\n" //
                        + "   v_color = s * quad_colors;\n" //
                        + "   gl_Position =  u_projTrans * vertex_positions;\n" //
                        + "}\n";
        final String fragmentShader = "#version 330 core\n" +
                "#ifdef GL_ES\n" //
                + "precision lowp float;\n" //
                + "#define MED mediump\n"
                + "#else\n"
                + "#define MED \n"
                + "#endif\n" //
                + "varying vec4 v_color;\n" //
                + "uniform sampler2D u_normals;\n" //
                + "uniform vec3 u_lightpos;\n" //
                + "uniform vec2 u_resolution;\n" //
                + "uniform float u_intensity = 1.0;\n" //
                + "void main()\n"//
                + "{\n"
                + "  vec2 screenPos = gl_FragCoord.xy / u_resolution.xy;\n"
                + "  vec3 NormalMap = texture2D(u_normals, screenPos).rgb; "
                + "  vec3 LightDir = vec3(u_lightpos.xy - screenPos, u_lightpos.z);\n"

                + "  vec3 N = normalize(NormalMap * 2.0 - 1.0);\n"

                + "  vec3 L = normalize(LightDir);\n"

                + "  float maxProd = max(dot(N, L), 0.0);\n"
                + "" //
                + "  gl_FragColor = v_color * maxProd * u_intensity;\n" //
                + "}";

        ShaderProgram.pedantic = false;
        ShaderProgram lightShader = new ShaderProgram(vertexShader,
                fragmentShader);
        if (!lightShader.isCompiled()) {
            Gdx.app.log("ERROR", lightShader.getLog());
        }

        lightShader.bind();
        lightShader.setUniformi("u_normals", 1);
        lightShader.setUniformf("u_resolution", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        return lightShader;
    }

}
