package io.github.theepicblock.polymc.impl.generator.asm.stack.ops;

import org.apache.commons.lang3.NotImplementedException;

import com.google.gson.JsonElement;

import io.github.theepicblock.polymc.impl.generator.asm.MethodExecutor.VmException;
import io.github.theepicblock.polymc.impl.generator.asm.VirtualMachine;
import io.github.theepicblock.polymc.impl.generator.asm.stack.KnownInteger;
import io.github.theepicblock.polymc.impl.generator.asm.stack.KnownObject;
import io.github.theepicblock.polymc.impl.generator.asm.stack.KnownVmObject;
import io.github.theepicblock.polymc.impl.generator.asm.stack.StackEntry;

public record InstanceOf(StackEntry entry, String toCheck) implements StackEntry {
    @Override
    public JsonElement toJson() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'toJson'");
    }

    @Override
    public <T> T cast(Class<T> type) {
        if (type == Integer.class) {
            try {
                return (T)(Integer)this.toInt();
            } catch (VmException e) {
                throw new NotImplementedException(e);
            }
        }
        return StackEntry.super.cast(type);
    }

    @Override
    public StackEntry resolve(VirtualMachine vm) throws VmException {
        return new KnownInteger(toInt(entry.resolve(vm), toCheck));
    }

    public int toInt() throws VmException {
        return toInt(this.entry, this.toCheck);
    }

    public static int toInt(StackEntry entry, String toCheck) throws VmException {
        if (entry instanceof KnownVmObject o) {
            return o.type().getNode().name.equals(toCheck) ? 1 : 0;
        } else if (entry instanceof KnownObject o) {
            if (o.i() == null) {
                return 0;
            } else {
                return o.i().getClass().getName().equals(toCheck) ? 1 : 0;
            }
        }
        throw new VmException("Can't find instance of "+entry, null);
    }
}