package com.eotv.echoofthevoid.lore;

import java.util.ArrayList;
import java.util.List;

/** Pure journal definitions and discovery rules shared by runtime and tests. */
public final class UncannyJournalCatalog {
    public static final int STORY_LENGTH_DAYS = 50;
    public static final int BEFORE_WINDOW_WEIGHT = 1;
    public static final int IN_WINDOW_WEIGHT = 6;
    public static final int AFTER_WINDOW_WEIGHT = 3;

    private static final List<Journal> JOURNALS = List.of(
            new Journal(
                    1,
                    "five torches",
                    2,
                    14,
                    List.of(
                            "i had 5 torches in here. one on every wall and one over the door. im writing it because i keep counting 4 and then 5 again",
                            "maybe i move them when i come back from mining. i do that with stuff sometimes. but i dont remember breaking any and there wasnt one in my inventory",
                            "last night the one over the door was on the other side. i saw it when i went to sleep. when i woke up it was back. i know beds turn you around so maybe that confused me",
                            "the door opened while i was making bread. not all the way. just the sound. i looked and it was shut",
                            "i put dirt under the 5th torch so i would know. this morning the dirt was under the 4th. i might have put it there. i was very tired",
                            "i asked in chat if someone joined. nobody answered. i checked the player list and it was only me but i still felt stupid for asking",
                            "im not counting them anymore. when i count them something always feels wrong and when i dont count them the room feels normal",
                            "there are 5 right now. im leaving this in the chest so i dont have to look again")),
            new Journal(
                    2,
                    "A Longer Way Home",
                    4,
                    20,
                    List.of(
                            "I moved because the old place was getting on my nerves. Nothing dramatic. Too many cave sounds under the floor, doors I was sure I had closed, that sort of thing.",
                            "The new camp is past the birch river and another full day east. No mineshaft on the map, no village, no reason for anyone to come through. It felt good for two nights.",
                            "Yesterday the leaves moved uphill from me in a line. Three trees, one after another. There was no wind mod. I checked my mod list twice anyway.",
                            "A fox came out of the last tree, which almost made me laugh. Then the same movement happened behind the fox while it was sitting still.",
                            "I went north instead of east this morning so I would not walk beside the trees. Near sunset I heard blocks breaking behind me. Dirt first, then stone. Slow, like somebody making stairs.",
                            "I waited with my axe out. The sound stopped close enough that I should have seen the particles. Nothing. When I kept walking it started far away again.",
                            "Maybe it is two normal things and I am joining them together because the first house scared me. I hope so. That would mean moving worked.",
                            "I can see the birch river from this hill. It should be west of me. It is south. I am taking the longer way home, even if the map fixes itself when I pick it up again.")),
            new Journal(
                    3,
                    "Room for One",
                    8,
                    28,
                    List.of(
                            "I built the upstairs room for Sam before remembering Sam is not on this world. I kept it anyway. It made the house look finished.",
                            "The bed is blue, the chest is empty and the painting is the little sunset one. I know the painting because I broke four ugly ones before I got it.",
                            "I came upstairs after feeding the cows and got the fighters painting instead. Same frame, same size. I touched it and the sunset came back without dropping anything.",
                            "One of the cows keeps looking at the upstairs window. The wolves do it too, but only from the bottom of the stairs. They are not angry. They just will not come up.",
                            "Things in the empty chest have moved twice. Nothing valuable. A bowl, seeds, one piece of string. They were mine, probably. I put them downstairs and found them upstairs again.",
                            "I shut the room and blocked the door with a stair. That was childish. The next morning the stair was still there and the door was open behind it.",
                            "Tonight I heard somebody get out of the bed. It made exactly the sound mine makes. The room was empty when I opened it, and the painting was the fighters again.",
                            "I took the bed away. The wolves came upstairs immediately. I wish that made me feel better.",
                            "I am leaving the room as it is. If I rebuild the wall I will keep wondering whether there used to be a door there.")),
            new Journal(
                    4,
                    "The Recipe Isn't There",
                    15,
                    40,
                    List.of(
                            "NEI showed two objects I had never seen in a pack before: an Altar and something called a Reality Cube. Both belonged to the same mod as the shards. Neither page showed a complete recipe.",
                            "I assumed it was hidden progression. That is normal enough. I kept the shards, searched the new ruins and tried the shapes that looked intentional. Most attempts did nothing.",
                            "The strange part was not finding the Altar. The strange part was checking NEI afterward. The Cube was gone from the list. I typed the same name, cleared the search and opened it again. It returned after I closed the inventory.",
                            "I eventually made both. I will not write the arrangement here because I changed three things at once and do not know which one mattered. Anyone claiming certainty from that would be guessing.",
                            "The Cube did not fire, glow or behave like a weapon. For a while, the room simply stayed the way I remembered it. The same painting. The same open door. Six items in the chest every time.",
                            "That quiet lasted until I slept. In the morning there were still six items, but they were in a different order. I laughed harder than it deserved. I think I was relieved that the change was small.",
                            "A loose page was under the Altar. It said, “im not counting them anymore.” The spelling and the crease were not mine. I had never seen the rest of that note.",
                            "I put the page in a barrel and went mining. It was back under the Altar when I returned. The barrel still contained a page too. I did not compare them closely enough before burning one.",
                            "I used the Altar once. Every sound in the base stopped, including the furnace. Then the furnace finished an item with no flame on its face. Nothing attacked me. Nothing asked whether I wanted to continue.",
                            "The silence may have meant it worked. It may have meant I had finally done the thing the world was waiting for. NEI still lists the Cube, except when I look for it directly.")),
            new Journal(
                    5,
                    "Under the Third Warning",
                    22,
                    50,
                    List.of(
                            "I know what a Warden sounds like. I have lost two sets of netherite to them and deserved it both times. What I found below the old tunnel was not one I had seen before.",
                            "The shrieker warned three times. After the third, the cave went quiet for a few seconds. Long enough for me to think it had failed. Then something breathed underneath me.",
                            "It came through the floor where there was no sculk. Taller than a Warden, or maybe I was crouching lower than I remember. Gold showed in the gaps around its chest and arms.",
                            "Not gold armor. It looked like the bright edge you get when two blocks do not quite meet, except the light stayed when it turned away.",
                            "I threw a snowball down the left passage. It followed the sound. That part was normal. I started sneaking right and it stopped at the place where I had been standing.",
                            "Then it copied my turn. I cannot explain that better. I moved my head toward the exit and a second later it faced the same direction, though I had made no sound.",
                            "I ran. That was the stupid part. It hit the wall behind me hard enough that I stopped thinking about whether it could see. I only remember the gold flashing closer each time it moved.",
                            "At the water tunnel it stopped following. I stayed underwater until I nearly drowned. When I came back, the cave was empty and a handful of shards were lying where it had first stood.",
                            "I took them. Of course I took them. If that was what it wanted, it chose the easiest person in the world to convince.",
                            "I blocked the tunnel and marked it with redstone. The mark is still there. Sometimes the blocks behind it make the warning sound in the wrong order.")),
            new Journal(
                    6,
                    "only dan heard it",
                    10,
                    45,
                    List.of(
                            "we all saw the tall thing by the wheat. me, dan and lou. it was there long enough that lou shot it and the arrow landed in the fence behind it",
                            "dan yelled before lou fired. he said it screamed right in his head. me and lou heard the bow and the cows but no scream",
                            "we thought he was messing with us until he took his headset off and still wouldnt go back outside. dan does dumb jokes but he doesnt do that",
                            "the thing came back by the upstairs window. this time we all heard knocking from the front door while we were looking at it. nobody wanted to turn around",
                            "when we did turn the door was open. lou says it was already open because she had just fed the wolves. i remember closing it after her. dan says there was never a door there",
                            "there is definitely a door. i can see it while writing this. we built the house together on day one",
                            "dan logged out. his armor stand is still in the corner but it has his skin when i look from the stairs. close up its normal armor. lou says it never changed",
                            "i asked her to write what she saw before i told her mine. she wrote “the window was empty.” i wrote “it ducked under the window.” both lines are on this page now",
                            "lou did not write on this page",
                            "im leaving the book here. if dan comes back im not asking what he heard. if he tells me anyway i dont think i want lou in the room")));

    private UncannyJournalCatalog() {
    }

    public static int count() {
        return JOURNALS.size();
    }

    public static List<Journal> journals() {
        return JOURNALS;
    }

    public static Journal journal(int index) {
        int clamped = Math.max(1, Math.min(index, JOURNALS.size()));
        return JOURNALS.get(clamped - 1);
    }

    public static int discoveryWeight(Journal journal, double logicalStoryDay) {
        if (logicalStoryDay < journal.windowStartDay()) {
            return BEFORE_WINDOW_WEIGHT;
        }
        if (logicalStoryDay <= journal.windowEndDay()) {
            return IN_WINDOW_WEIGHT;
        }
        return AFTER_WINDOW_WEIGHT;
    }

    public static int selectMissing(int foundMask, int maximumJournals, double logicalStoryDay, double roll) {
        if (!Double.isFinite(roll) || roll < 0.0D || roll >= 1.0D) {
            throw new IllegalArgumentException("roll must be in [0, 1)");
        }
        int limit = Math.max(0, Math.min(Math.min(maximumJournals, JOURNALS.size()), 30));
        List<WeightedJournal> eligible = new ArrayList<>();
        int totalWeight = 0;
        for (int index = 1; index <= limit; index++) {
            int bit = 1 << (index - 1);
            if ((foundMask & bit) != 0) {
                continue;
            }
            Journal journal = journal(index);
            int weight = discoveryWeight(journal, logicalStoryDay);
            eligible.add(new WeightedJournal(index, weight));
            totalWeight += weight;
        }
        if (eligible.isEmpty()) {
            return -1;
        }
        int target = Math.min(totalWeight - 1, (int) Math.floor(roll * totalWeight));
        for (WeightedJournal candidate : eligible) {
            if (target < candidate.weight()) {
                return candidate.index();
            }
            target -= candidate.weight();
        }
        return eligible.get(eligible.size() - 1).index();
    }

    public record Journal(int index, String title, int windowStartDay, int windowEndDay, List<String> pages) {
        public Journal {
            if (index <= 0 || title == null || title.isBlank() || windowStartDay < 0
                    || windowEndDay < windowStartDay || pages == null || pages.isEmpty()) {
                throw new IllegalArgumentException("Invalid journal definition");
            }
            pages = List.copyOf(pages);
        }
    }

    private record WeightedJournal(int index, int weight) {
    }
}
