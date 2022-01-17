package me.ztiany.asm.agenttest;

/**
 * 测试 AsmAgent 生成的 javaagent。
 *
 * @author ztiany
 * Email: ztiany3@gmail.com
 */
public class AsmAgentMain {

    /*
    java5: run with the following argument:
        1. edit configurations
        2. modify options
        3. add jvm options: -javaagent:D:\code\ztiany\Java-base-code\ASM-Base\AsmAgent\build\libs\AsmAgent-1.0.jar
    */
    public static void main(String... args) {
        System.out.println("AsmAgentMain running");
        User user = new User();
        user.setAge(12);
        user.setName("Ztiany");
        System.out.println(user);
    }

    /*java6*/
    public static void _main(String... args) throws Error {
        //VirtualMachine.attach();
        //VirtualMachine.loadAgent("xxx.jar");
        //VirtualMachine vm = VirtualMachine.attach();
        //vm.loadAgent(jarFilePath, args);
    }

}
