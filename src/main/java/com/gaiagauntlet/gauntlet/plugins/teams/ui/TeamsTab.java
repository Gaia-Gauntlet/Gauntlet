package com.gaiagauntlet.gauntlet.plugins.teams.ui;

import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.events.AdminPageEvent;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.pages.AdminPage;
import com.gaiagauntlet.gauntlet.core.ui.pages.Widgets;
import com.gaiagauntlet.gauntlet.plugins.teams.TeamsPlugin;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamListComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamType;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.Universe;

/**
 * The selected session's roster. Players are picked from who is online, every team is its own block
 * with online and offline members told apart, and members can be assigned or removed.
 */
public final class TeamsTab implements AdminTab {

    private static final String TEAM_ROW = "Gauntlet/Plugins/" + TeamsPlugin.ID + "/TeamRow.ui";

    /** The team ids the rows were built for, so rows are rebuilt only when teams come or go. */
    @Nullable private List<String> rowTeams;

    /** Members each row's picker holds, so a picker is refilled only when its team changes. */
    private final Map<String, List<UUID>> pickerMembers = new HashMap<>();
    private List<UUID> pickerUnassigned = List.of();

    @Nonnull @Override public String getId() {
        return "Teams";
    }

    @Nonnull @Override public String getPanel() {
        return "Gauntlet/Plugins/" + TeamsPlugin.ID + "/TeamsPanel.ui";
    }

    @Override public int getOrder() {
        return 20;
    }

    @Override
    public void bind(@Nonnull UIEventBuilder evt) {
        Widgets.bindValues(evt, "#AssignGo", "teams.assign", "",
                Map.of("@Pick", "#AssignPlayer.Value", "@Text", "#AssignName.Value", "@Num", "#AssignTeam.Value"));
        Widgets.bindValues(evt, "#NewTeamGo", "teams.create", "",
                Map.of("@Text", "#NewTeamId.Value", "@Pick", "#NewTeamName.Value", "@Num", "#NewTeamSize.Value"));
        Widgets.bindValues(evt, "#DeleteTeamGo", "teams.delete", "", Map.of("@Pick", "#DeleteTeamPicker.Value"));
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
        rowTeams = null;
        pickerMembers.clear();
        pickerUnassigned = List.of();
        fillTeamPickers(cmd, TeamUi.teamsOf(session));
    }

    @Override
    public void render(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
        var teamList = TeamUi.teamsOf(session);

        var unassigned = new ArrayList<UUID>();
        for (var player : Universe.get().getPlayers()) {
            if (teamList == null || teamList.get(player.getUuid()) == null) {
                unassigned.add(player.getUuid());
            }
        }
        unassigned.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(TeamUi.nameOf(a), TeamUi.nameOf(b)));
        if (!unassigned.equals(pickerUnassigned)) {
            pickerUnassigned = List.copyOf(unassigned);
            Widgets.fillPicker(cmd, "#AssignPlayer", memberOptions(pickerUnassigned));
        }
        Widgets.field(cmd, "UnassignedField", unassigned.isEmpty() ? "everyone online is on a team" : unassigned.size() + " online without a team");

        var teams = teamList == null ? List.<TeamComponent>of() : TeamUi.sortedTeams(teamList);
        var ids = teams.stream().map(TeamComponent::getId).toList();
        if (!ids.equals(rowTeams)) {
            pickerMembers.clear();
            cmd.clear("#TeamList");
            for (int i = 0; i < teams.size(); i++) {
                var row = "#TeamList[" + i + "]";
                cmd.append("#TeamList", TEAM_ROW);
                Widgets.bindValues(evt, row + " #Remove", "teams.remove", teams.get(i).getId(), Map.of("@Pick", row + " #Member.Value"));
                Widgets.bindArg(evt, row + " #Send", "teams.send", teams.get(i).getId());
                Widgets.bindArg(evt, row + " #Toggle", "teams.toggle", teams.get(i).getId());
            }
            fillTeamPickers(cmd, teamList);
            if (teams.isEmpty()) {
                Widgets.fillList(cmd, "TeamList", List.of(), "No teams yet");
            }
        }

        for (int i = 0; i < teams.size(); i++) {
            var team = teams.get(i);
            var row = "#TeamList[" + i + "]";
            var members = TeamUi.membersOf(team);

            int onlineCount = 0;
            Message memberText = null;
            for (var member : members) {
                boolean here = PlayerUtils.isOnline(member);
                onlineCount += here ? 1 : 0;
                var part = Message.raw((memberText == null ? "" : ", ") + TeamUi.nameOf(member)).color(here ? "#c9d1d9" : "#5a6a7a");
                memberText = memberText == null ? part : memberText.insert(part);
            }

            Widgets.text(cmd, row + " #Name", TeamUi.displayName(team));
            var status = Message.raw(team.getSize() + " players  ·  " + onlineCount + " online").color("#878e9c");
            if (team.getTeamType() != TeamType.Participant) {
                status = status.insert(Message.raw("  ·  " + team.getTeamType().name().toLowerCase()).color("#878e9c"));
            }
            cmd.set(row + " #Status.TextSpans", status);
            if (memberText == null) {
                Widgets.text(cmd, row + " #Members", "(empty)");
            } else {
                cmd.set(row + " #Members.TextSpans", memberText);
            }
            if (!members.equals(pickerMembers.get(team.getId()))) {
                pickerMembers.put(team.getId(), List.copyOf(members));
                Widgets.fillPicker(cmd, row + " #Member", memberOptions(members));
            }
        }
        rowTeams = List.copyOf(ids);
    }

    @Nullable
    @Override
    public Message handle(@Nonnull String action, @Nonnull AdminPageEvent event, @Nullable GameSession session, @Nonnull AdminPage page) {
        return switch (action) {
            case "teams.assign" -> assign(event, session, page);
            case "teams.remove" -> remove(event, session);
            case "teams.create" -> create(event, session);
            case "teams.delete" -> error("Deleting teams is not yet supported!");
            case "teams.send" -> error("Sending a team into the game is not yet supported!");
            case "teams.toggle" -> error("Sitting a team out is not yet supported!");
            default -> null;
        };
    }

    @Nullable
    private Message assign(@Nonnull AdminPageEvent event, @Nullable GameSession session, @Nonnull AdminPage page) {
        var teams = TeamUi.teamsOf(session);
        if (teams == null) {
            return error("This session has no teams yet!");
        }
        var team = teams.get(event.num());
        if (team == null) {
            return error("Pick a team");
        }
        if (!event.text().isEmpty()) {
            var name = event.text();
            PlayerUtils.uuidOf(name).whenComplete((uuid, failure) -> page.pushStatus(failure != null || uuid == null
                    ? Widgets.fail("Could not find a player named " + name)
                    : put(teams, uuid, team)));
            return Widgets.ok("Looking up " + name);
        }
        var uuid = event.pick().isEmpty() ? null : TeamComponent.parseUuid(event.pick());
        if (uuid == null) {
            return error("Pick a player or type a name");
        }
        return put(teams, uuid, team);
    }

    @Nonnull
    private static Message put(@Nonnull TeamListComponent teams, @Nonnull UUID player, @Nonnull TeamComponent team) {
        teams.put(player, team.getId());
        return Widgets.ok(TeamUi.nameOf(player) + " is now on " + TeamUi.displayName(team));
    }

    @Nonnull
    private static Message remove(@Nonnull AdminPageEvent event, @Nullable GameSession session) {
        var teams = TeamUi.teamsOf(session);
        var uuid = event.pick().isEmpty() ? null : TeamComponent.parseUuid(event.pick());
        if (teams == null || uuid == null) {
            return error("Pick a member");
        }
        var team = teams.get(event.arg());
        if (team == null || !team.contains(uuid)) {
            return Widgets.warn(TeamUi.nameOf(uuid) + " was not on " + event.arg());
        }
        teams.remove(uuid, team.getId());
        return Widgets.ok(TeamUi.nameOf(uuid) + " removed from " + TeamUi.displayName(team));
    }

    @Nonnull
    private static Message create(@Nonnull AdminPageEvent event, @Nullable GameSession session) {
        if (session == null) {
            return error("Pick a session first");
        }
        var id = event.text();
        if (id.isEmpty() || id.contains(" ")) {
            return error("Give the team an id without spaces");
        }
        var type = TeamListComponent.getSessionComponentType();
        var teams = session.get(type).orElseGet(() -> {
            var created = new TeamListComponent();
            session.put(type, created);
            return created;
        });
        if (teams.get(id) != null) {
            return error("Team " + id + " already exists");
        }
        int size = event.num().isEmpty() ? 0 : Widgets.parseInt(event.num(), "players");
        var name = event.pick().isEmpty() ? id.replace('_', ' ') : event.pick();
        teams.addTeam(id, new TeamComponent(id, name, TeamType.Participant, size, null));
        return Widgets.ok("Created team " + name);
    }

    private void fillTeamPickers(@Nonnull UICommandBuilder cmd, @Nullable TeamListComponent teamList) {
        var options = new ArrayList<Widgets.Option>();
        if (teamList != null) {
            for (var team : TeamUi.sortedTeams(teamList)) {
                options.add(new Widgets.Option(TeamUi.displayName(team), team.getId()));
            }
        }
        Widgets.fillPicker(cmd, "#AssignTeam", options);
        Widgets.fillPicker(cmd, "#DeleteTeamPicker", options);
    }

    @Nonnull
    private static List<Widgets.Option> memberOptions(@Nonnull List<UUID> players) {
        var options = new ArrayList<Widgets.Option>(players.size());
        for (var player : players) {
            options.add(new Widgets.Option(TeamUi.nameOf(player), player.toString()));
        }
        return options;
    }
}
