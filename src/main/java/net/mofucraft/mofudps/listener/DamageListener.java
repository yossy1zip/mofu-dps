package net.mofucraft.mofudps.listener;

import net.mofucraft.mofudps.dps.DpsManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * プレイヤーがMob(プレイヤー以外のLivingEntity)に与えたダメージを検知し、
 * DpsManagerへ計上する。
 */
public final class DamageListener implements Listener {

    private final DpsManager dpsManager;

    public DamageListener(DpsManager dpsManager) {
        this.dpsManager = dpsManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        if (!(victim instanceof LivingEntity) || victim instanceof Player) {
            return;
        }

        Entity causingEntity = event.getDamageSource().getCausingEntity();
        if (!(causingEntity instanceof Player attacker)) {
            return;
        }

        double damage = event.getFinalDamage();
        if (damage <= 0) {
            return;
        }

        dpsManager.recordDamage(attacker, damage);
    }
}
