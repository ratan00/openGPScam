import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

// Top level, not a field of the factory: Gradle only isolates the factory as managed state if it
// has no instance fields of its own.
private val targets = mapOf(
    "org.fossify.commons.activities.BaseSimpleActivity" to
        setOf("onCreate" to "org.fossify.", "startCustomizationActivity" to "yfissof"),
    "org.fossify.commons.compose.extensions.ActivityExtensionsKt" to
        setOf("fakeVersionCheck" to "org.fossify."),
)

/**
 * The Fossify commons library shows a "fake version of the app" warning in every app whose package
 * does not start with "org.fossify.". This fork has its own package, so the warning is always a false
 * alarm. The three check sites below compare against an empty string instead, so they pass.
 */
abstract class FakeVersionCheckPatch : AsmClassVisitorFactory<InstrumentationParameters.None> {

    override fun isInstrumentable(classData: ClassData): Boolean = classData.className in targets

    override fun createClassVisitor(classContext: ClassContext, nextClassVisitor: ClassVisitor): ClassVisitor {
        val patches = targets.getValue(classContext.currentClassData.className)
        return object : ClassVisitor(Opcodes.ASM9, nextClassVisitor) {
            override fun visitMethod(
                access: Int,
                name: String,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?,
            ): MethodVisitor {
                val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
                val literals = patches.filter { it.first == name }.map { it.second }.toSet()
                if (literals.isEmpty()) return mv
                return object : MethodVisitor(Opcodes.ASM9, mv) {
                    override fun visitLdcInsn(value: Any?) {
                        super.visitLdcInsn(if (value is String && value in literals) "" else value)
                    }
                }
            }
        }
    }
}
