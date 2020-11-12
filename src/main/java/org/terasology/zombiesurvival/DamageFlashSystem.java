package org.terasology.zombiesurvival;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.health.event.OnDamagedEvent;
import org.terasology.logic.players.PlayerCharacterComponent;
import org.terasology.registry.In;
import org.terasology.rendering.nui.NUIManager;

@RegisterSystem(RegisterMode.CLIENT)
public class DamageFlashSystem extends BaseComponentSystem implements UpdateSubscriberSystem {

    private static final Logger logger = LoggerFactory.getLogger(DamageFlashSystem.class);

    @In
    private NUIManager nuiManager;

    @In
    private Time time;

    private boolean waitingEnd = false;
    private long gameTimeScreenUp = 0L;
    private long msDisplayFlash = 150L;

    // TODO Must be a better way to do this
    @ReceiveEvent(components = PlayerCharacterComponent.class)
    public void onDamaged(OnDamagedEvent event, EntityRef entity) {
        if (!waitingEnd) {
            waitingEnd = true;
            gameTimeScreenUp = time.getGameTimeInMs();
            nuiManager.addOverlay("ZombieSurvival:damageFlash", DamageFlash.class);
        }
    }

    @Override
    public void update(float delta) {
        if (waitingEnd && gameTimeScreenUp + msDisplayFlash < time.getGameTimeInMs()) {
            nuiManager.removeOverlay("ZombieSurvival:damageFlash");
            waitingEnd = false;
        }
    }
}
