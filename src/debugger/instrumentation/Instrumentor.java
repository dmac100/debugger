package debugger.instrumentation;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.field.FieldDescription.InDefinedShape;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.Implementation.Context;
import net.bytebuddy.pool.TypePool;

public class Instrumentor {
	static {
		ByteBuddyAgent.install();
	}
	
	private static class VisitorWrapper implements AsmVisitorWrapper {
		@Override
		public int mergeReader(int flags) {
			return flags | ClassReader.EXPAND_FRAMES;
		}

		@Override
		public int mergeWriter(int flags) {
			return flags | ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS;
		}
		
		public ClassVisitor wrap(TypeDescription instrumentedType, ClassVisitor classVisitor, Context context, TypePool typePool, FieldList<InDefinedShape> fieldList, MethodList<?> methodList, int writerFlags, int readFlags) {
			return new InstrumentorClassVisitor(classVisitor);
		}
	}
	
	public void instrumentClass(Class<?> clazz) {
		new ByteBuddy()
			.redefine(clazz)
			.visit(new VisitorWrapper())
			.make()
			.load(getClass().getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
	}
}