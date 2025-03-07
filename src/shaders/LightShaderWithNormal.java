package shaders;

import box2dLight.RayHandler;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public final class LightShaderWithNormal {
    public static ShaderProgram createLightShader() {
        // Shader adapted from https://github.com/mattdesl/lwjgl-basics/wiki/ShaderLesson6
        String gamma = "";
        if (RayHandler.getGammaCorrection())
            gamma = "sqrt";
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
        final String fragmentShader =
                "#version 330 core\n" +
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
                        + "uniform vec2 u_world;\n" //
                        + "uniform float u_intensity;\n" //
                        + "uniform vec3 u_falloff;"
                        + "void main()\n"//
                        + "{\n" //
                        + "  vec2 screenPos = gl_FragCoord.xy / u_resolution.xy;\n"
                        + "  vec4 NormalMapTexture = texture2D(u_normals, screenPos);\n"
                        + "  vec3 NormalMap = NormalMapTexture.rgb;\n"
                        + "  float alpha = NormalMapTexture.a;\n"
                        + "  vec3 LightDir = vec3(u_lightpos.xy - screenPos, u_lightpos.z);\n"
                        + "  LightDir.xy *= u_world.xy;\n"
                        + "  float D = length(LightDir);\n"
                        + "  float Attenuation = 1.0 / (u_falloff.x + (u_falloff.y*D) + (u_falloff.z*D*D));\n"
                        + "  vec3 N = normalize(NormalMap * 2.0 - 1.0);\n"
                        + "  vec3 L = normalize(LightDir);\n"
                        + "  float maxProd = (max(dot(N, L), 0.0) * Attenuation - 1.0) * alpha + 1.0;\n"
//                + "  gl_FragColor = NormalMapTexture;\n" //
//                        +"  gl_FragColor = vec4(screenPos.x,screenPos.y,1,1);\n" //
//                        +"  gl_FragColor = vec4(screenPos.y,screenPos.y,screenPos.y,1);\n" //
                        + "  gl_FragColor = " + gamma + "(v_color * maxProd * u_intensity);\n" //
                        + "}";

        ShaderProgram.pedantic = false;
        ShaderProgram lightShader = new ShaderProgram(vertexShader,
                fragmentShader);
        if (!lightShader.isCompiled()) {
            Gdx.app.log("ERROR", lightShader.getLog());
        }

//        lightShader.bind();
//        lightShader.setUniformi("u_normals", 1);
//        lightShader.setUniformf("u_resolution", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        return lightShader;
    }

}
