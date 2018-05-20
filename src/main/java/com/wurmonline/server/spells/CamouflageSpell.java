package com.wurmonline.server.spells;

import com.joedobo27.c.ConfigureOptions;
import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.deities.Deities;
import com.wurmonline.server.deities.Deity;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemSpellEffects;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.shared.constants.Enchants;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import static com.joedobo27.c.CamouflageSpellActionPerformer.getCamouflageSpellActionPerformer;

public class CamouflageSpell extends ReligiousSpell {

    private CamouflageSpell() {
        super("Camouflage", getCamouflageSpellActionPerformer().getActionId(),
                ConfigureOptions.getInstance().getCastingEarthSeconds(),
                ConfigureOptions.getInstance().getCostFavor(),
                ConfigureOptions.getInstance().getSpellDifficulty(),
                ConfigureOptions.getInstance().getRequiredFaith(),
                ConfigureOptions.getInstance().getCoolDownEarthMilliseconds());
        this.targetItem = true;
        this.enchantment = Enchants.CRET_ILLUSION;
        this.effectdesc = "will help hide you from monsters.";
        this.description = "will help hide you from monsters";
    }

    private static class SingletonHelper {
        private static final CamouflageSpell _spell;
        static {
            _spell = new CamouflageSpell();
        }
    }

    public static CamouflageSpell getCamouflageSpell(){
        return CamouflageSpell.SingletonHelper._spell;
    }

    public static void build() {
        CamouflageSpell camouflageSpell = getCamouflageSpell();
        Spells.addSpell(camouflageSpell);
        Deity vynora = Deities.getDeity(Deities.DEITY_VYNORA);
        vynora.addSpell(camouflageSpell);
        camouflageSpell.setType((byte)1);
    }

    public static void updateSpell() {
        Spell spell = getCamouflageSpell();
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (spell) {
            try {
                ReflectionUtil.setPrivateField(spell,
                        ReflectionUtil.getField(Class.forName("com.wurmonline.server.spells.Spell"), "castingTime"),
                        ConfigureOptions.getInstance().getCastingEarthSeconds());
                ReflectionUtil.setPrivateField(spell,
                        ReflectionUtil.getField(Class.forName("com.wurmonline.server.spells.Spell"), "cost"),
                        ConfigureOptions.getInstance().getCostFavor());
                ReflectionUtil.setPrivateField(spell,
                        ReflectionUtil.getField(Class.forName("com.wurmonline.server.spells.Spell"), "difficulty"),
                        ConfigureOptions.getInstance().getSpellDifficulty());
                ReflectionUtil.setPrivateField(spell,
                        ReflectionUtil.getField(Class.forName("com.wurmonline.server.spells.Spell"), "level"),
                        ConfigureOptions.getInstance().getRequiredFaith());
                ReflectionUtil.setPrivateField(spell,
                        ReflectionUtil.getField(Class.forName("com.wurmonline.server.spells.Spell"), "cooldown"),
                        ConfigureOptions.getInstance().getCoolDownEarthMilliseconds());
            }catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
                logger.warning("Failed updating spell" + e.getMessage());
            }
        }

    }

    @Override
    boolean precondition(final Skill castSkill, final Creature performer, final Item target) {
        if (!target.isArmour()){
            performer.getCommunicator().sendNormalServerMessage("Only armor will accept Camouflage.", (byte)3);
            return false;
        }
        return true;
    }

    @Override
    boolean precondition(final Skill castSkill, final Creature performer, final Creature target) {
        return false;
    }

    @Override
    void doEffect(final Skill castSkill, final double power, final Creature performer, final Item target) {
        if (!Spell.mayBeEnchanted(target)) {
            performer.getCommunicator().sendNormalServerMessage("The spell fizzles.", (byte)3);
            return;
        }
        ItemSpellEffects effs = target.getSpellEffects();
        if (effs == null) {
            effs = new ItemSpellEffects(target.getWurmId());
        }
        SpellEffect eff = effs.getSpellEffect(this.enchantment);
        if (eff == null) {
            performer.getCommunicator().sendNormalServerMessage(
                    "The " + target.getName() + " will now camouflage you.", (byte)2);
            eff = new SpellEffect(target.getWurmId(), this.enchantment, (float)power, 20000000);
            effs.addSpellEffect(eff);
            Server.getInstance().broadCastAction(performer.getName() + " looks pleased.", performer, 5);
        }
        else if (eff.getPower() > power) {
            performer.getCommunicator().sendNormalServerMessage(
                    "You frown as you fail to improve the power.", (byte)3);
            Server.getInstance().broadCastAction(performer.getName() + " frowns.", performer, 5);
        }
        else {
            performer.getCommunicator().sendNormalServerMessage(
                    "You succeed in improving the power of the " + this.name + ".", (byte)2);
            eff.improvePower((float)power);
            Server.getInstance().broadCastAction(performer.getName() + " looks pleased.", performer, 5);
        }
    }

    @Override
    void doNegativeEffect(final Skill castSkill, final double power, final Creature performer, final Item target) {
        this.checkDestroyItem(power, performer, target);
    }

}
