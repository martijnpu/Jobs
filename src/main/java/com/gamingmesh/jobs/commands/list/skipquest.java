package com.gamingmesh.jobs.commands.list;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.commands.Cmd;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobsPlayer;
import com.gamingmesh.jobs.container.Quest;
import com.gamingmesh.jobs.container.QuestProgression;
import com.gamingmesh.jobs.economy.BufferedEconomy;
import com.gamingmesh.jobs.i18n.Language;

import net.Zrips.CMILib.Locale.LC;

public class skipquest implements Cmd {

    @Override
    public Boolean perform(Jobs plugin, final CommandSender sender, final String[] args) {
        if (!Jobs.getGCManager().DailyQuestsEnabled) {
            LC.info_FeatureNotEnabled.sendMessage(sender);
            return null;
        }

        if (args.length != 2 && args.length != 3) {
            return false;
        }

        JobsPlayer jPlayer = null;
        Job job = null;
        String questName = "";

        for (String one : args) {
            if (job == null) {
                job = Jobs.getJob(one);
                if (job != null)
                    continue;
            }
            if (jPlayer == null) {
                jPlayer = Jobs.getPlayerManager().getJobsPlayer(one);
                if (jPlayer != null)
                    continue;
            }

            if (!questName.isEmpty())
                questName += " ";
            questName += one;
        }

        if (jPlayer == null && sender instanceof Player)
            jPlayer = Jobs.getPlayerManager().getJobsPlayer((Player) sender);

        if (jPlayer == null) {
            Language.sendMessage(sender, "general.error.noinfoByPlayer", "%playername%", args.length > 0 ? args[0] : "");
            return null;
        }

        List<QuestProgression> quests = jPlayer.getQuestProgressions();

        if (job != null)
            quests = jPlayer.getQuestProgressions(job);

        if (quests == null || quests.isEmpty()) {
            Language.sendMessage(sender, "command.resetquest.output.noQuests");
            return null;
        }

        Quest old = null;

        for (QuestProgression one : quests) {
            Quest q = one.getQuest();

            if (q.getQuestName().equalsIgnoreCase(questName) || q.getConfigName().equalsIgnoreCase(questName)) {
                old = q;
                break;
            }
        }

        if (old == null) {
            return false;
        }

        // Do not skip the completed quests
        for (QuestProgression q : quests) {
            if (q.getQuest().getQuestName().equals(old.getQuestName()) && q.isCompleted()) {
                return false;
            }
        }

        if (Jobs.getGCManager().getDailyQuestsSkips() <= jPlayer.getSkippedQuests()) {
            return false;
        }

        double amount = Jobs.getGCManager().skipQuestCost;
        BufferedEconomy econ = Jobs.getEconomy();
        Player player = jPlayer.getPlayer();

        if (amount > 0 && player != null) {
            if (!econ.getEconomy().hasMoney(player, amount)) {
                Language.sendMessage(sender, "economy.error.nomoney");
                return null;
            }

            econ.getEconomy().withdrawPlayer(player, amount);
        }

        jPlayer.replaceQuest(old);

        if (player != null)
            plugin.getServer().dispatchCommand(player, "jobs quests");

        if (amount > 0) {
            Language.sendMessage(sender, "command.skipquest.output.questSkipForCost", "%amount%", amount);
        }

        return true;
    }
}
