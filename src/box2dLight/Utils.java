package box2dLight;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class Utils {

    /** The code that is always added to the vertex shader code.
     * Note that this is added as-is, you should include a newline (`\n`) if needed. */
    public static String prependVertexCode = "";

    /** The code that is always added to every fragment shader code.
     * Note that this is added as-is, you should include a newline (`\n`) if needed. */
    public static String prependFragmentCode = "";
    public static ShaderProgram compileShader(FileHandle vertexFile, FileHandle fragmentFile) {
        return compileShader(vertexFile, fragmentFile, "");
    }

    public static ShaderProgram compileShader(FileHandle vertexFile, FileHandle fragmentFile, String defines) {
        if (fragmentFile == null) {
            throw new IllegalArgumentException("Vertex shader file cannot be null.");
        }
        if (vertexFile == null) {
            throw new IllegalArgumentException("Fragment shader file cannot be null.");
        }
        if (defines == null) {
            throw new IllegalArgumentException("Defines cannot be null.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Compiling \"").append(vertexFile.name()).append('/').append(fragmentFile.name()).append('\"');
        if (defines.length() > 0) {
            sb.append(" w/ (").append(defines.replace("\n", ", ")).append(")");
        }
        sb.append("...");
        Gdx.app.log("box2dLight", sb.toString());

        String prependVert = prependVertexCode + defines;
        String prependFrag = prependFragmentCode + defines;
        String srcVert = vertexFile.readString();
        String srcFrag = fragmentFile.readString();

        ShaderProgram shader = new ShaderProgram(prependVert + "\n" + srcVert, prependFrag + "\n" + srcFrag);

        if (!shader.isCompiled()) {
            throw new GdxRuntimeException("Shader compile error: " + vertexFile.name() + "/" + fragmentFile.name() + "\n" + shader.getLog());
        }
        return shader;
    }
}
