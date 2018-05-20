package com.joedobo27.c;

import com.wurmonline.server.Server;
import com.wurmonline.server.combat.ArmourTypes;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoArmourException;
import com.wurmonline.server.items.NoSpaceException;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.spells.CamouflageSpell;
import com.wurmonline.server.zones.VirtualZone;
import com.wurmonline.shared.constants.BodyPartConstants;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;
import javassist.bytecode.*;
import org.gotti.wurmunlimited.modloader.classhooks.CodeReplacer;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.*;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class CamouflageMod implements WurmServerMod, Initable, ServerStartedListener, Configurable, PlayerMessageListener {

    static final Logger logger = Logger.getLogger(CamouflageMod.class.getName());

    private boolean putHookInstalled = false;

    @Override
    public void configure(Properties properties) {
        ConfigureOptions.setOptions(properties);
    }

    @Override
    public MessagePolicy onPlayerMessage(Communicator communicator, String message, String title) {
        if (communicator.getPlayer().getPower() == 5 && message.startsWith("/CamouflageMod properties") &&
                putHookInstalled) {
            communicator.getPlayer().getCommunicator().sendNormalServerMessage(
                    "Reloading properties for CamouflageMod."
            );
            ConfigureOptions.resetOptions();
            CamouflageSpell.updateSpell();
            return MessagePolicy.DISCARD;
        }
        return MessagePolicy.PASS;
    }

    @Override
    public boolean onPlayerMessage(Communicator communicator, String s) {
        return false;
    }

    @Override
    public void init() {
        try {
            CtClass ctClassVirtualZone = HookManager.getInstance().getClassPool()
                    .get("com.wurmonline.server.zones.VirtualZone");
            CtMethod ctMethodAddCreature = ctClassVirtualZone.getMethod("addCreature",
                    Descriptor.ofMethod(CtPrimitiveType.booleanType, new CtClass[]{CtPrimitiveType.longType,
                            CtPrimitiveType.booleanType, CtPrimitiveType.longType, CtPrimitiveType.floatType,
                            CtPrimitiveType.floatType, CtPrimitiveType.floatType}));


            // Construct a byte array to find in addCreature's bytecode and get the table line number where that found
            // byte array is at.
            ctClassVirtualZone.getClassFile().compact();
            Bytecode find = new Bytecode(ctClassVirtualZone.getClassFile().getConstPool());
            find.addAload(0);
            find.addGetfield("com.wurmonline.server.zones.VirtualZone", "creatures", "Ljava/util/Map;");
            find.addLload(1);
            find.addInvokestatic("java.lang.Long", "valueOf", "(J)Ljava/lang/Long;");
            find.addConstZero(HookManager.getInstance().getClassPool().get(
                    "com.wurmonline.server.creatures.CreatureMove"));
            find.addInvokeinterface("java.util.Map", "put",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 3);
            find.addOpcode(Opcode.POP);

            int lineNumber = byteArrayToLineNumber(find.get(), ctMethodAddCreature, 15);

            Bytecode replace = new Bytecode(ctClassVirtualZone.getClassFile().getConstPool());
            replace.addAload(0);
            replace.addLload(1);
            replace.addInvokestatic("com.joedobo27.c.CamouflageMod", "canCreatureCamouflagedInVisionZone",
                    "(Lcom/wurmonline/server/zones/VirtualZone;J)Z");
            codeBranching(replace, Opcode.IFNE, 18);
            replace.addAload(0);
            replace.addGetfield("com.wurmonline.server.zones.VirtualZone", "creatures", "Ljava/util/Map;");
            replace.addLload(1);
            replace.addInvokestatic("java.lang.Long", "valueOf", "(J)Ljava/lang/Long;");
            replace.addConstZero(HookManager.getInstance().getClassPool().get(
                    "com.wurmonline.server.creatures.CreatureMove"));
            replace.addInvokeinterface("java.util.Map", "put",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 3);
            replace.addOpcode(Opcode.POP);


            CodeReplacer codeReplacer = new CodeReplacer(ctMethodAddCreature.getMethodInfo().getCodeAttribute());
            codeReplacer.replaceCode(find.get(), replace.get());
            ctMethodAddCreature.getMethodInfo().rebuildStackMapIf6(ctClassVirtualZone.getClassPool(), ctClassVirtualZone.getClassFile());
            ctClassVirtualZone.debugWriteFile("C:\\Users\\Jason\\Documents\\WU\\WU-Server\\byte code prints\\");

        }catch (NotFoundException | BadBytecode | RuntimeException e){
            logger.warning(e.getMessage() + " FAILURE  installing hook on put() in VirtualZone.addCreature().");
        }
        putHookInstalled = true;
    }

    @Override
    public void onServerStarted() {
        if (putHookInstalled) {
            CamouflageSpellActionPerformer camouflageSpellActionPerformer =
                    CamouflageSpellActionPerformer.getCamouflageSpellActionPerformer();
            ModActions.registerAction(camouflageSpellActionPerformer);
            ModActions.registerAction(camouflageSpellActionPerformer.getActionEntry());

            CamouflageSpell.build();
        }
    }


    @SuppressWarnings("unused")
    public static boolean canCreatureCamouflagedInVisionZone(VirtualZone visionZone, long creatureId){
        Creature creature = Server.getInstance().getCreatureOrNull(creatureId);
        Creature watcher = visionZone.getWatcher();
        if (watcher == null || watcher instanceof Player || watcher.isGuard() || creature == null
                || creature.getTarget() != null || !(creature instanceof Player))
           return false;

        ArrayList<Byte> armorPositions = new ArrayList<>(Arrays.asList(
                BodyPartConstants.HEAD, BodyPartConstants.LEFT_ARM, BodyPartConstants.LEFT_HAND,
                BodyPartConstants.RIGHT_ARM, BodyPartConstants.RIGHT_HAND, BodyPartConstants.TORSO,
                BodyPartConstants.LEGS, BodyPartConstants.LEFT_FOOT, BodyPartConstants.RIGHT_FOOT));
        double camouflagePowerTally = armorPositions.stream()
                .mapToDouble(armorPos -> {
                    try{
                        return creature.getArmour(armorPos).getBonusForSpellEffect((byte)72);
                    }catch (NoArmourException | NoSpaceException e) {
                        return 0.0D;
                    }
                })
                .average()
                .orElse(0.0D);
        if (camouflagePowerTally == 0.0D)
            return false;

        boolean isCamouflageOnCoolDown = creature.hasBeenAttackedWithin((int)Math.ceil(
                ConfigureOptions.getInstance().getCamouflageRecoveryScale().doFunctionOfX(camouflagePowerTally)));

        boolean spellPowerCamouflageFailure = Server.rand.nextInt(100) + 1 > Math.min(100, Math.max(1,
                        ConfigureOptions.getInstance().getSpellPowerExplainsCamouflageChance()
                                .doFunctionOfX(camouflagePowerTally)));

        double armorLevel = armorPositions.stream()
                .mapToDouble(armorPos -> {
                    try{
                        return ArmourTypes.getArmourBaseDR(creature.getArmour(armorPos).getArmourType());
                    }catch (NoArmourException | NoSpaceException e) {
                        return 0.0D;
                    }
                })
                .average()
                .orElse(0.0D);
        boolean armorDRCamouflageFailure = Server.rand.nextInt(100) >= 100 - Math.min(100, Math.max(0,
                ConfigureOptions.getInstance().getArmorDRExplainsCamouflageChance().doFunctionOfX(armorLevel)));


        return (!isCamouflageOnCoolDown && !spellPowerCamouflageFailure && !armorDRCamouflageFailure);
    }


    @SuppressWarnings("SameParameterValue")
    private int byteArrayToLineNumber(byte[] bytesSeek, CtMethod ctMethod, int byteArraySize)
            throws BadBytecode, RuntimeException {

        // Using bytesSeek iterate through the ctMethod's bytecode looking for a matching byte array sized to byteArraySize
        int bytecodeIndex = -1;
        CodeIterator codeIterator = ctMethod.getMethodInfo().getCodeAttribute().iterator();
        codeIterator.begin();
        long find = byteArrayToLong(bytesSeek);
        while (codeIterator.hasNext() && codeIterator.lookAhead() + byteArraySize < codeIterator.getCodeLength()) {
            int index = codeIterator.next();
            byte[] bytesFound = new byte[byteArraySize];
            for(int i=0;i<byteArraySize;i++){
                bytesFound[i] = (byte)codeIterator.byteAt(index + i);
            }
            long found = byteArrayToLong(bytesFound);
            if (found == find) {
                bytecodeIndex = index;
            }
        }
        if (bytecodeIndex == -1)
            throw new RuntimeException("no bytecode match found.");
        // Get the line number table entry for the bytecodeIndex.
        LineNumberAttribute lineNumberAttribute = (LineNumberAttribute) ctMethod.getMethodInfo().getCodeAttribute()
                .getAttribute(LineNumberAttribute.tag);
        int lineNumber = lineNumberAttribute.toLineNumber(bytecodeIndex);
        int lineNumberTableOrdinal =  IntStream.range(0, lineNumberAttribute.tableLength())
                .filter(value -> Objects.equals(lineNumberAttribute.lineNumber(value), lineNumber))
                .findFirst()
                .orElseThrow(RuntimeException::new);
        return lineNumberAttribute.lineNumber(lineNumberTableOrdinal);
    }


    private static long byteArrayToLong(byte[] bytesOriginal) {
        if (bytesOriginal.length < 8) {
            byte[] bytesLongPadded = new byte[8];
            System.arraycopy(bytesOriginal, 0, bytesLongPadded, 8 - bytesOriginal.length,
                    bytesOriginal.length);
            return ByteBuffer.wrap(bytesLongPadded).getLong();
        }
        else
            return ByteBuffer.wrap(bytesOriginal).getLong();
    }

    private static void codeBranching(Bytecode bytecode, int opcode, int branchCount){
        bytecode.addOpcode(opcode);
        bytecode.add((branchCount >>> 8) & 0xFF, branchCount & 0xFF);
    }

}
