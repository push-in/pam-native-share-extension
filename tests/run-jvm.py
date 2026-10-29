from pathlib import Path
import subprocess,os
r=Path(__file__).resolve().parents[1]
c=Path.home()/'.gradle/caches/modules-2/files-2.1'
def jar(group,name,version=None):
    root=c/group/name
    if version: root=root/version
    return str(sorted(root.glob('**/*.jar'))[-1])
compiler=[jar('org.jetbrains.kotlin', n,'2.2.10') for n in ['kotlin-compiler-embeddable','kotlin-stdlib','kotlin-script-runtime','kotlin-daemon-embeddable']]
compiler += [jar('org.jetbrains.kotlin','kotlin-reflect'),jar('org.jetbrains.intellij.deps','trove4j'),jar('org.jetbrains.kotlinx','kotlinx-coroutines-core-jvm'),jar('org.jetbrains','annotations')]
libs=[jar('org.jetbrains.kotlin','kotlin-stdlib','2.2.10'), jar('junit','junit','4.13.2'),jar('org.hamcrest','hamcrest-core'),jar('org.json','json','20250517'),str(Path.home()/'Android/Sdk/platforms/android-35/android.jar')]
api=Path(os.environ['PAM_NATIVE_SOURCE'])/'android/plugin-api/src/main/java'
fw=api
sources=list((r/'android/src/main/kotlin').rglob('*.kt'))+list((r/'android/src/test/kotlin').rglob('*.kt'))
sources+=list((api/'dev/pam/nativeapp/protocol').glob('*.kt'))+[api/'dev/pam/nativeapp/modules/NativeModule.kt',fw/'dev/pam/nativeapp/modules/SurfaceRetirableModule.kt',fw/'dev/pam/nativeapp/HostEvents.kt']
out=r/'build/jvm';out.mkdir(parents=True,exist_ok=True)
if __name__ == "__main__":
    subprocess.run(['java','-Xmx3g','-cp',':'.join(compiler),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',':'.join(libs),'-d',str(out),*map(str,sources)],check=True)
    subprocess.run(['java','-Xmx3g','-cp',':'.join([str(out),*libs]),'org.junit.runner.JUnitCore','dev.pam.shareextension.ShareLimitsTest','dev.pam.shareextension.ReloadListenersTest'],check=True)
