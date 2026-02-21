package net.mexish.libs.command.test.type.command;

import net.mexish.libs.command.annotation.ArgumentSignature;
import net.mexish.libs.command.context.CommandContext;
import net.mexish.libs.command.annotation.NamedSubcommand;
import net.mexish.libs.command.executor.CommandExecutor;
import net.mexish.libs.command.annotation.NamedCommand;
import net.mexish.libs.command.executor.SubcommandExecutor;


@NamedCommand("niggerparent")
public final class NiggerCommand {

    @NamedSubcommand("niggersub1")
    public final class NiggerNiggerNiggerCommand {

        @NamedSubcommand("niggersub2")
        public final class NiggerNiggerCommand {
            @CommandExecutor
            public static void onNiggerSub2(final CommandContext ctx) {
                System.out.println("niggersub2");
            }

            @SubcommandExecutor("nigger")
            public static void onNiggerSub2Nigger(final CommandContext ctx) {
                System.out.println("niggersub2 nigger");
            }

            @NamedSubcommand("faggot")
            public final class NiggerFaggotCommand {
                @CommandExecutor
                public static void onNiggerFaggot(final CommandContext ctx) {
                    System.out.println("niggersub2 niggerfaggot");
                }
            }
        }

//        @ArgumentSignature("int int int int")
//        @CommandExecutor
//        public static void onNiggerNigger(final CommandContext ctx) {
//            System.out.println("niggersub"); // this is equal to onNiggerNigger in the superclass
//        }

        @SubcommandExecutor("niggersubsub")
        @ArgumentSignature("int int")
        public static void onNiggerNiggerNigger(final CommandContext ctx) {
            System.out.println("niggersubsub");
        }

    }

    @CommandExecutor
    public static void onNigger(final CommandContext ctx) {
        System.out.println("NIGGGEEER: " + ctx.getWrapper().getName());
    }

    @SubcommandExecutor("niggersub")
    public static void onNiggerNigger(final CommandContext ctx) {
        System.out.println("nigger nigger called");
    }

    @SubcommandExecutor("niggersubtest1")
    public static void onNiggerNiggertest1(final CommandContext ctx) {
        System.out.println("nigger nigger test1 called");
    }

}
