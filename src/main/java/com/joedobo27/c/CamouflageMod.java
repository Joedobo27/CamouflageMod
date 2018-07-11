package com.joedobo27.c;

import com.wurmonline.server.Server;
import com.wurmonline.server.combat.ArmourTypes;
import com.wurmonline.server.creatures.*;
import com.wurmonline.server.items.NoSpaceException;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.spells.CamouflageSpell;
import com.wurmonline.server.zones.VirtualZone;
import com.wurmonline.shared.constants.BodyPartConstants;
import javassist.*;
import javassist.bytecode.*;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.*;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class CamouflageMod implements WurmServerMod, Initable, ServerStartedListener, Configurable,
        PlayerMessageListener {

    static final Logger logger = Logger.getLogger(CamouflageMod.class.getName());

    private static boolean putHookInstalled = false;

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
            byteVirtualZone();
            byteCreature();
        }catch (NotFoundException | CannotCompileException | BadBytecode e){
            logger.warning(e.getMessage() + " FAILURE  modifying VirtualZone class methods.");
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

    /**
     * search for this.creatures.put in VirtualZone to find what adds creatures to vision area
     * and add in a method call hook right after each to remove camouflaged players. Some of the
     * calls to the put arn't for players in a creatures vision.
     * @throws NotFoundException JA exceptions.
     */
    private static void byteVirtualZone() throws NotFoundException, BadBytecode, CannotCompileException {
        ClassPool classPool = HookManager.getInstance().getClassPool();
        CtClass virtualZone = classPool.get("com.wurmonline.server.zones.VirtualZone");
        virtualZone.getClassFile().compact();

        // Add a call to my custom method to handle maybe removing creatures from vision area.
        CtMethod addCreature = virtualZone.getMethod("addCreature", Descriptor.ofMethod(
                CtPrimitiveType.booleanType, new CtClass[]{ CtPrimitiveType.longType, CtPrimitiveType.booleanType,
                        CtPrimitiveType.longType,
                CtPrimitiveType.floatType, CtPrimitiveType.floatType, CtPrimitiveType.floatType
        }));
        Bytecode find = new Bytecode(virtualZone.getClassFile().getConstPool());
        find.addAload(0);
        find.addGetfield("com/wurmonline/server/zones/VirtualZone", "creatures", "Ljava/util/Map;");
        find.addLload(1);
        find.addInvokestatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
        find.addConstZero(classPool.get("com/wurmonline/server/creatures/CreatureMove"));
        find.addInvokeinterface("java/util/Map", "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 3);
        find.addOpcode(Opcode.POP);
        int tableLine = byteArrayToLineNumber(find.get(), addCreature.getMethodInfo().getCodeAttribute(), 1);
        addCreature.insertAt(tableLine, "com.joedobo27.c.CamouflageMod#removeFromVisionZone($0, $0.creatures, creature);");

        // Add a call to my custom method to handle maybe removing creatures from vision area.
        CtMethod addItem = HookManager.getInstance().getClassPool().get("com.wurmonline.server.zones.VirtualZone")
                .getDeclaredMethod("addItem", new CtClass[]{
                        HookManager.getInstance().getClassPool().get("com.wurmonline.server.items.Item"),
                        HookManager.getInstance().getClassPool().get("com.wurmonline.server.zones.VolaTile"),
                        CtPrimitiveType.longType, CtPrimitiveType.booleanType
                });
        find = new Bytecode(virtualZone.getClassFile().getConstPool());
        find.addAload(0);
        find.addGetfield("com/wurmonline/server/zones/VirtualZone", "creatures", "Ljava/util/Map;");
        find.addLload(3);
        find.addInvokestatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
        find.addConstZero(classPool.get("com/wurmonline/server/creatures/CreatureMove"));
        find.addInvokeinterface("java/util/Map", "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 3);
        find.addOpcode(Opcode.POP);
        tableLine = byteArrayToLineNumber(find.get(), addItem.getMethodInfo().getCodeAttribute(), 1);
        addItem.insertAt(tableLine, "com.joedobo27.c.CamouflageMod#removeFromVisionZone($0, $0.creatures, $3);");

        // Add a call to my custom method to handle maybe removing creatures from vision area.
        CtMethod addCreatureToMap = HookManager.getInstance().getClassPool().get(
                "com.wurmonline.server.zones.VirtualZone").getDeclaredMethod("addCreatureToMap");
        find = new Bytecode(virtualZone.getClassFile().getConstPool());
        find.addAload(0);
        find.addGetfield("com/wurmonline/server/zones/VirtualZone", "creatures", "Ljava/util/Map;");
        find.addAload(1);
        find.addInvokevirtual("com/wurmonline/server/creatures/Creature", "getWurmId", "()J");
        find.addInvokestatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
        find.addAload(2);
        find.addInvokeinterface("java/util/Map", "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 3);
        find.addOpcode(Opcode.POP);
        tableLine = byteArrayToLineNumber(find.get(), addCreatureToMap.getMethodInfo().getCodeAttribute(), 1);
        addCreatureToMap.insertAt(tableLine,
                "com.joedobo27.c.CamouflageMod#removeFromVisionZone($0, $0.creatures, $1);");
    }

    private static void byteCreature() throws NotFoundException, CannotCompileException {
        ClassPool classPool = HookManager.getInstance().getClassPool();
        CtClass creature = classPool.get("com.wurmonline.server.creatures.Creature");

        CtMethod attackTarget = creature.getMethod("attackTarget", Descriptor.ofMethod(
                CtPrimitiveType.voidType, null));
        attackTarget.insertBefore("" +
                "if (com.joedobo27.c.CamouflageMod#shouldCamouflageCancelAttack($0, $0.target))" +
                "   return;");

        CtMethod setTarget = creature.getMethod("setTarget", Descriptor.ofMethod(
                CtPrimitiveType.voidType,  new CtClass[]{CtPrimitiveType.longType, CtPrimitiveType.booleanType}));
        setTarget.insertBefore("" +
                "if (com.joedobo27.c.CamouflageMod#shouldCamouflageCancelAttack($0, $1))" +
                "   return;");
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static boolean shouldCamouflageCancelAttack(Creature watcher, long targetId) {
        VisionArea visionArea = watcher.getVisionArea();
        if (watcher == null || visionArea == null)
            return false;
        VirtualZone virtualZone;
        if (watcher.isOnSurface())
            virtualZone = visionArea.getSurface();
        else
            virtualZone = visionArea.getUnderGround();

        if (!canCreatureCamouflagedInVisionZone(virtualZone, targetId))
            return false;
        watcher.removeTarget(targetId);

        Creature player = Server.getInstance().getCreatureOrNull(targetId);
        if (!(player instanceof Player))
            return false;
        HashMap<Long, CreatureMove> creatures;
        try {
            creatures = ReflectionUtil.getPrivateField(virtualZone, ReflectionUtil.getField(
                    Class.forName("com.wurmonline.server.zones.VirtualZone"), "creatures"));
        }catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            return false;
        }
        boolean isTargeting = watcher.getTarget() != null && watcher.getTarget().getWurmId() == targetId;
        creatures.remove(targetId);
        if (isTargeting)
            watcher.removeTarget(targetId);
        if (Objects.equals(watcher.opponent, Creatures.getInstance().getCreatureOrNull(targetId)))
            watcher.setOpponent(null);
        return true;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static void removeFromVisionZone(VirtualZone virtualZone, Map<Long, Creature> creatureMap,
                                                         long creatureId) {
        removeFromVisionZone(virtualZone, creatureMap, Server.getInstance().getCreatureOrNull(creatureId));
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public synchronized static void removeFromVisionZone(VirtualZone virtualZone, Map<Long, Creature> creatureMap,
                                                         Creature creature) {
        if (creatureMap.containsKey(creature.getWurmId()) && canCreatureCamouflagedInVisionZone(virtualZone,
                creature.getWurmId())){
            creatureMap.remove(creature.getWurmId());
            Creature watcher = virtualZone.getWatcher();
            boolean isTargeting = watcher.getTarget() != null && watcher.getTarget().getWurmId() == creature.getWurmId();
            if (isTargeting)
                watcher.removeTarget(creature.getWurmId());
            if (Objects.equals(watcher.opponent, creature))
                watcher.setOpponent(null);
        }
    }

    private static boolean canCreatureCamouflagedInVisionZone(VirtualZone visionZone, long creatureId){
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
        int rollSpellPower = Server.rand.nextInt(100) + 1;
        double spellPowerScaled = ConfigureOptions.getInstance().getSpellPowerExplainsCamouflageChance()
                .doFunctionOfX(camouflagePowerTally);
        boolean spellPowerCamouflageFailure = rollSpellPower > Math.min(100, Math.max(1, spellPowerScaled));

        double recoveryScaled = ConfigureOptions.getInstance().getCamouflageRecoveryScale().doFunctionOfX(camouflagePowerTally);
        boolean isCamouflageOnCoolDown = creature.hasBeenAttackedWithin((int)Math.ceil(recoveryScaled));

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
        int rollArmor = Server.rand.nextInt(100);
        double armorScaled = ConfigureOptions.getInstance().getArmorDRExplainsCamouflageChance().doFunctionOfX(armorLevel);
        boolean armorDRCamouflageFailure = rollArmor >= 100 - Math.min(100, Math.max(0, armorScaled));

        return (!isCamouflageOnCoolDown && !spellPowerCamouflageFailure && !armorDRCamouflageFailure);
    }

    @SuppressWarnings("SameParameterValue")
    private static int byteArrayToLineNumber(byte[] bytesSeek, CodeAttribute codeAttribute, int offset)
            throws BadBytecode, RuntimeException {

        // Using bytesSeek iterate through the ctMethod's bytecode looking for a matching byte array sized to byteArraySize
        int bytecodeIndex = -1;
        CodeIterator codeIterator = codeAttribute.iterator();
        codeIterator.begin();
        while (codeIterator.hasNext() && codeIterator.lookAhead() + bytesSeek.length < codeIterator.getCodeLength()) {
            int index = codeIterator.next();
            byte[] bytesFound = new byte[bytesSeek.length];
            for(int i=0;i<bytesSeek.length;i++){
                bytesFound[i] = (byte)codeIterator.byteAt(index + i);
            }
            if (Arrays.equals(bytesSeek, bytesFound)) {
                bytecodeIndex = index;
            }
        }
        if (bytecodeIndex == -1)
            throw new RuntimeException("no bytecode match found.");
        // Get the line number table entry for the bytecodeIndex.
        LineNumberAttribute lineNumberAttribute = (LineNumberAttribute) codeAttribute.getAttribute(LineNumberAttribute.tag);
        int lineNumber = lineNumberAttribute.toLineNumber(bytecodeIndex);
        int lineNumberTableOrdinal =  IntStream.range(0, lineNumberAttribute.tableLength())
                .filter(value -> Objects.equals(lineNumberAttribute.lineNumber(value), lineNumber))
                .findFirst()
                .orElseThrow(RuntimeException::new) + offset;
        return lineNumberAttribute.lineNumber(lineNumberTableOrdinal);
    }
}
