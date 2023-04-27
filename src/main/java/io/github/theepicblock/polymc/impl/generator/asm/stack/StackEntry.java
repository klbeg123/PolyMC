package io.github.theepicblock.polymc.impl.generator.asm.stack;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import io.github.theepicblock.polymc.impl.generator.asm.MethodExecutor.VmException;
import io.github.theepicblock.polymc.impl.generator.asm.VirtualMachine;

public interface StackEntry {
    static final Gson GSON = new Gson();

    static @NotNull StackEntry knownStackValue(Object o) {
        if (o instanceof Integer i) {
            return new KnownInteger(i);
        }
        if (o instanceof Float f) {
            return new KnownFloat(f);
        }
        if (o instanceof Type t) {
            return new KnownClass(t);
        }
        return new KnownObject(o);
    }

    default void setField(String name, StackEntry e) {
        throw new NotImplementedException("Can't set field "+name+" on "+this);
    }

    default @NotNull StackEntry getField(String name) throws VmException {
        throw new NotImplementedException("Can't get field "+name+" from "+this);
    }

    default @NotNull StackEntry arrayAccess(int index) throws VmException {
        throw new NotImplementedException("Can't load an array index ("+index+") from "+this);
    }

    default void arraySet(int index, @NotNull StackEntry entry) throws VmException {
        throw new NotImplementedException("Can't load an array index ("+index+") from "+this);
    }

    /**
     * For stack entries that represent delayed instructions, such as {@link StaticFieldValue}
     */
    default StackEntry resolve(VirtualMachine vm) throws VmException {
        return this;
    }

    JsonElement toJson();

    /**
     * Extracts the value of this entry into a POJO
     */
    default <T> T cast(Class<T> type) {
        return GSON.fromJson(this.toJson(), type);
    }
}