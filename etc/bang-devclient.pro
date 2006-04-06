#
# $Id$
#
# Proguard configuration file for Bang! development client

-injars /export/tools/lib/java/commons-io.jar(!META-INF/*)
-injars /export/tools/lib/java/commons-collections.jar(!META-INF/*)
-injars ../lib/getdown.jar(!META-INF/*,!**/tools/**)
-injars ../lib/narya-base.jar(!META-INF/*,!**/tools/**)
-injars ../lib/narya-distrib.jar(!META-INF/*,!**/tools/**)
-injars ../lib/narya-media.jar(!META-INF/*,!**/tools/**)
-injars ../lib/narya-parlor.jar(!META-INF/*,!**/tools/**)
-injars ../lib/narya-cast.jar(!META-INF/*,!**/tools/**)
-injars ../lib/narya-jme.jar(!META-INF/*,!**/tools/**)
-injars ../lib/narya-openal.jar(!META-INF/*,!**/tools/**)
-injars ../lib/samskivert.jar(!META-INF/*,!**/velocity/**,!**/xml/**)
-injars ../lib/jme.jar(!META-INF/*)
-injars ../lib/jme-effects.jar(!META-INF/*)
-injars ../lib/jme-model.jar(!META-INF/*)
-injars ../lib/jme-terrain.jar(!META-INF/*)
-injars ../lib/jme-awt.jar(!META-INF/*)
-injars ../lib/jme-sound.jar(!META-INF/*,!**/fmod/**)
-injars ../lib/jme-bui.jar(!META-INF/*,!**/tests/**)
-injars ../lib/threerings.jar(**/coin/**)
-injars ../lib/hemiptera.jar(!**/web/**,!**/persist/**,!install.properties)
-injars ../lib/jorbis-0.0.12.jar(!META-INF/*)
-injars ../lib/jogg-0.0.5.jar(!META-INF/*)
-injars ../dist/bang-code.jar(!META-INF/*,!**/tools/**,!**/tests/**)

-libraryjars <java.home>/lib/rt.jar
-libraryjars ../lib/lwjgl.jar

# -dontoptimize
# -dontobfuscate

-outjars ../dist/bang-pcode.jar
-printseeds ../dist/proguard.seeds
-printmapping proguard/devclient.map
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    !static !transient <fields>;
    private void writeObject (java.io.ObjectOutputStream);
    private void readObject (java.io.ObjectInputStream);
}

-keep public class * extends com.threerings.presents.dobj.DObject {
    !static !transient <fields>;
}
-keep public class * implements com.threerings.io.Streamable {
    !static !transient <fields>;
    <init> ();
    public void readObject (com.threerings.io.ObjectInputStream);
    public void writeObject (com.threerings.io.ObjectOutputStream);
}

-keep public class * extends java.lang.Enum {
    *;
}

-keep public class com.threerings.media.tile.SwissArmyTileSet
-keep public class com.threerings.media.tile.TrimmedTileSet

-keep public class * extends com.samskivert.swing.Controller {
    *;
}

-keep public class com.threerings.bang.client.BangApp {
    public static void main (java.lang.String[]);
}

-keep public class com.threerings.bang.editor.EditorApp {
    public static void main (java.lang.String[]);
}

-keep public class com.threerings.bang.editor.EditorManager {
    *;
}

-keep public class com.threerings.bang.viewer.ViewerApp {
    public static void main (java.lang.String[]);
}

-keep public class com.threerings.bang.data.Badge {
    static <fields>;
}

-keep public class com.threerings.bang.data.Stat {
    static <fields>;
}
