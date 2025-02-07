package shaders;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public final class NormalShader {
    public static ShaderProgram createNormalShader() {
        String vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
                + "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
                + "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
                + "uniform mat4 u_projTrans;\n" //
                + "varying vec4 v_color;\n" //
                + "varying vec2 v_texCoords;\n" //
                + "\n" //
                + "void main()\n" //
                + "{\n" //
//                + "   v_rot = mat2(u_projTrans[0].xy,u_projTrans[1].xy);\n" //
//                + "   v_rot = mat2(0,-1,1,0);\n" //
                + "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
                + "   v_color.a = v_color.a * (255.0/254.0);\n" //
                + "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
                + "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
                + "}\n";
        String fragmentShader = "#ifdef GL_ES\n" //
                + "#define LOWP lowp\n" //
                + "precision mediump float;\n" //
                + "#else\n" //
                + "#define LOWP \n" //
                + "#endif\n" //
                + "uniform vec2 u_rot;\n" //
                + "varying LOWP vec4 v_color;\n" //
                + "varying vec2 v_texCoords;\n" //
                + "uniform sampler2D u_texture;\n" //
                + "void main()\n"//
                + "{\n" //
                + "  mat2 rot = mat2(u_rot.x, u_rot.y, -u_rot.y, u_rot.x);\n" //
                + "  vec4 normal = texture2D(u_texture, v_texCoords).rgba;\n" //
                // got to translate normal vector to -1, 1 range
                + "  vec2 rotated = rot * (normal.xy * 2.0 - 1.0);\n" //
                // and back to 0, 1
                + "  rotated = (rotated.xy / 2.0 + 0.5 );\n" //
                + "  gl_FragColor = vec4(rotated.xy, normal.z, normal.a);\n" //
                + "}";

        ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);
        if (!shader.isCompiled()) throw new IllegalArgumentException("Error compiling shader: " + shader.getLog());
        return shader;
    }

}
