package io.github.theepicblock.polymc.impl.generator.asm;

import io.github.theepicblock.polymc.impl.generator.asm.MethodExecutor.VmException;
import io.github.theepicblock.polymc.impl.generator.asm.VirtualMachine.Clazz;
import io.github.theepicblock.polymc.impl.generator.asm.VirtualMachine.Context;
import io.github.theepicblock.polymc.impl.generator.asm.stack.KnownObject;
import io.github.theepicblock.polymc.impl.generator.asm.stack.KnownVmObject;
import io.github.theepicblock.polymc.impl.generator.asm.stack.MockedField;
import io.github.theepicblock.polymc.impl.generator.asm.stack.StackEntry;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;

public class AsmUtils {
    /**
     * @deprecated This method is really slow, only use it in non-critical sections
     */
    @Deprecated
    public static @Nullable MethodNode getMethod(ClassNode node, String name, String desc) {
        return node.methods
            .stream()
            .filter(m -> (m.name.equals(name) && m.desc.equals(desc)))
            .findFirst()
            .orElse(null);
    }

    /**
     * Converts an internal name (as returned by {@link org.objectweb.asm.Type#getInternalName})
     * to a binary name (like the one {@link ClassLoader#loadClass(String)} expects)
     */
    public static String toBinary(String internalName) {
        return internalName.replace("/", ".");
    }

    /**
     * Checks the *actual* jvm to see if the static field referenced by {@code inst} is already loaded
     */
    public static @Nullable StackEntry tryGetStaticFieldFromEnvironment(Context ctx, FieldInsnNode inst) {
        try {
            /*
            // Only if the class is already loaded
            var m = AsmUtils.class.getClassLoader().getClass().getDeclaredMethod("findLoadedClassFwd", new Class[] { String.class });
            m.setAccessible(true);
            ClassLoader cl = AsmUtils.class.getClassLoader();
            var clazz = m.invoke(cl, inst.owner.replace("/", "."));
            if (clazz != null) {
                return new KnownObject(((Class<?>)clazz).getField(inst.name).get(null));
            }
            */

            // Will error if this class can't be loaded from the environment
            return new KnownObject(Class.forName(inst.owner.replace("/", ".")).getField(inst.name).get(null));
        } catch (Throwable e) {}
        return null;
    }

    public static boolean hasFlag(int bitfield, int flag) {
        return (bitfield & flag) == flag;
    }

    public static MappedFunction map(String className, String methodName, String descriptor) {
        return map(FabricLoader.getInstance().getMappingResolver(), className, methodName, descriptor);
    }

    public static MappedFunction map(MappingResolver resolver, String className, String methodName, String descriptor) {
        var newClassname = resolver.mapClassName("intermediary", className);
        var newMethodName = resolver.mapMethodName("intermediary", className, methodName, descriptor);
        var newDesc = Mapping.remapDescriptor(s -> resolver.mapClassName("intermediary", s), descriptor);
        return new MappedFunction(newClassname.replace(".", "/"), newMethodName, newDesc);
    }

    public record MappedFunction(@InternalName String clazz, String method, String desc) {
    }

    /**
     * Convenience method to create a vm object for a class.
     * This is useful when {@link KnownObject}'s can't be used (such as when the object is client-only)
     * @param className The name of the class in intermediary
     * @throws VmException
     */
    public static VmObjectBuilder constructVmObject(VirtualMachine vm, String className) throws VmException {
        var runtimeName = FabricLoader.getInstance().getMappingResolver().mapClassName("intermediary", className);
        var clazz = vm.getClass(runtimeName.replace(".", "/"));

        return new VmObjectBuilder(clazz);
    }
    public static VmObjectBuilder constructVmObject(VirtualMachine vm, Class<?> clazzName) throws VmException {
        var clazz = vm.getClass(clazzName.getName().replace(".", "/"));

        return new VmObjectBuilder(clazz);
    }

    public static class VmObjectBuilder {
        private final Clazz clazz;
        private final HashMap<String, StackEntry> fields = new HashMap<>();
        private final KnownVmObject object;

        public VmObjectBuilder(Clazz clazz) {
            this.clazz = clazz;
            this.object = new KnownVmObject(clazz, fields);
        }

        public VmObjectBuilder mockField(String name) {
            // TODO this bad, please create a `MockedObject` instead, k thx
            this.fields.put(name, new MockedField(this.object, name));
            return this;
        }

        public VmObjectBuilder f(String name, Object object) {
            this.fields.put(name, StackEntry.known(object));
            return this;
        }

        public KnownVmObject build() {
            return new KnownVmObject(clazz, fields);
        }
    }
}
