package com.joedobo27.c;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.spells.CamouflageSpell;
import com.wurmonline.server.spells.Spell;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;


public class CamouflageSpellActionPerformer implements ModAction, ActionPerformer {

    private final ActionEntry actionEntry;
    private final int actionId;

    private CamouflageSpellActionPerformer(int actionId, ActionEntry actionEntry) {
        this.actionId = actionId;
        this.actionEntry = actionEntry;
    }

    @Override
    public short getActionId() {
        return (short)actionId;
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Item target, short actionId, float counter) {
        Spell camouflage = CamouflageSpell.getCamouflageSpell();
        return camouflage.run(performer, target, counter);
    }

    private static class SingletonHelper {
        private static final CamouflageSpellActionPerformer _performer;
        static {
            int configureActionId = ModActions.getNextActionId();
            _performer = new CamouflageSpellActionPerformer(configureActionId, ActionEntry.createEntry(
                    (short)configureActionId, "Camouflage","casting",
                    new int[]{2, 36, 48}));
        }
    }

    public static CamouflageSpellActionPerformer getCamouflageSpellActionPerformer(){
        return SingletonHelper._performer;
    }

    ActionEntry getActionEntry() {
        return actionEntry;
    }
}
