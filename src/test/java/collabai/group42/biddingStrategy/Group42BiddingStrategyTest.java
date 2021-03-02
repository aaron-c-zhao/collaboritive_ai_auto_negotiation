package collabai.group42.biddingStrategy;

import collabai.group42.opponent.Group42OpponentModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import geniusweb.actions.Action;
import geniusweb.actions.PartyId;
import collabai.group42.BoaState;
import geniusweb.inform.Settings;
import geniusweb.issuevalue.Bid;
import geniusweb.opponentmodel.OpponentModel;
import geniusweb.profile.Profile;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.progress.ProgressRounds;
import geniusweb.references.Parameters;
import geniusweb.references.ProfileRef;
import geniusweb.references.ProtocolRef;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import tudelft.utilities.immutablelist.ImmutableList;
import tudelft.utilities.logging.Reporter;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class Group42BiddingStrategyTest {

    private static final double EPSILON = 1e-4;
    private static final PartyId PARTY1 = new PartyId("party1");
    private static final String PROFILE = "src/test/resources/testprofile.json";
    private static final String SAOP = "SAOP";
    private final ProtocolRef protocol = new ProtocolRef(SAOP);
    private final Parameters parameters = new Parameters();
    private LinearAdditive profile;
    private final static ObjectMapper jackson = new ObjectMapper();

    private Group42BiddingStrategy biddingStrategy;
    @Mock
    private BoaState boaState;
    private Settings settings;
    @Mock
    private ProgressRounds progress;
    @Mock
    private Reporter reporter;


    @Before
    public void setup() throws URISyntaxException, IOException {
        biddingStrategy = spy(new Group42BiddingStrategy());
        settings = new Settings(PARTY1, new ProfileRef(new URI("file:" + PROFILE))
                , protocol, progress, parameters);
        String serialized = new String(Files.readAllBytes(Paths.get(PROFILE)),
                StandardCharsets.UTF_8);
        profile = (LinearAdditive) jackson.readValue(serialized, Profile.class);
        when(boaState.getSettings()).thenReturn(settings);
        when(boaState.getProfile()).thenReturn(profile);
        when(boaState.getProgress()).thenReturn(progress);
        when(boaState.getReporter()).thenReturn(reporter);
        doNothing().when(reporter).log(any(), any());

        // first need getAction to initialize the bidspace
        when(progress.get(any())).thenReturn(0.05);
        Action action = biddingStrategy.getAction(boaState);
    }


    @Test
    public void testProgressFirstPhase() {
        Group42BiddingStrategy bsInit = new Group42BiddingStrategy();
        when(progress.get(any())).thenReturn(0.05);
        Action action = bsInit.getAction(boaState);
        System.out.println(bsInit.getTargetUtility(0.05));
        assertEquals(0.995 * bsInit.getMax(), bsInit.getTargetUtility(0.05), EPSILON);
    }

    @Test
    public void testTargetUtilitySecondPhaseToughOpponent() {
        double progress = 0.5;
        doReturn(0.0).when(biddingStrategy).getNiceness();
        biddingStrategy.setB();
        double expected = (0.99 - Math.pow((progress - biddingStrategy.getB()), biddingStrategy.getA()))
                * biddingStrategy.getMax();
        assertEquals(expected, biddingStrategy.getTargetUtility(progress), EPSILON);
    }

    @Test
    public void testTargetUtilitySecondPhaseNiceOpponent() {
        double progress = 0.5;
        doReturn(0.3).when(biddingStrategy).getNiceness();
        biddingStrategy.setB();
        double expected = (0.99 - Math.pow((progress - biddingStrategy.getB()), biddingStrategy.getA()))
                * biddingStrategy.getMax();

        assertEquals(expected, biddingStrategy.getTargetUtility(0.5), EPSILON);
    }

    @Test
    public void testTargetUtilityThirdPhaseToughOpponent() {
        double progress = 0.9;
        doReturn(0.0).when(biddingStrategy).getNiceness();

        biddingStrategy.setA();
        double expected = (0.99 - Math.pow((progress - biddingStrategy.getB()), biddingStrategy.getA()))
                * biddingStrategy.getMax();
        assertEquals(expected, biddingStrategy.getTargetUtility(progress), EPSILON);
    }

    @Test
    public void testTargetUtilityThirdPhaseNiceOpponent() {
        double progress = 0.9;
        doReturn(0.4).when(biddingStrategy).getNiceness();

        biddingStrategy.setA();
        double expected = (0.99 - Math.pow((progress - biddingStrategy.getB()), biddingStrategy.getA()))
                * biddingStrategy.getMax();
        assertEquals(expected, biddingStrategy.getTargetUtility(progress), EPSILON);
    }

    @Test
    public void testGetMin() {
        assertEquals(biddingStrategy.getMin(), 0.24, EPSILON);
    }

    @Test
    public void testGetUtility() {
        assertEquals(biddingStrategy.getUtility(biddingStrategy.getBidSpace().getRvBid()), 0.0, EPSILON);
    }

    @Test
    public void testTargetUtilityLastPhase() {
        assertEquals(biddingStrategy.getMin(), biddingStrategy.getTargetUtility(0.999), EPSILON);
    }

    @Test
    public void testGetNiceness() {
        LinkedList<List<Double>> mockList1 = new LinkedList<>();
        LinkedList<List<Double>> mockList2 = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            mockList1.add(new ArrayList<>(Arrays.asList(i * 0.01, 0.2 - i * 0.01)));
            mockList2.add(new ArrayList<>(Arrays.asList(1 - i * 0.01, 0.2)));
        }

        doReturn(mockList1).when(biddingStrategy).getRecentBids();
        assertEquals(1, biddingStrategy.getNiceness(), EPSILON);

        doReturn(mockList2).when(biddingStrategy).getRecentBids();
        assertEquals(0.0, biddingStrategy.getNiceness(), EPSILON);
    }

    @Test
    public void testSetBAndGetB() {
        doReturn(0.2).when(biddingStrategy).getNiceness();
        biddingStrategy.setB();
        assertEquals(0.1 + 0.1 * biddingStrategy.getNiceness(), biddingStrategy.getB(), EPSILON);
    }

    @Test
    public void testSetAAndGetA() {
        doReturn(0.2).when(biddingStrategy).getNiceness();
        biddingStrategy.setA();
        assertEquals(4.0 + 2.0 * biddingStrategy.getNiceness(), biddingStrategy.getA(), EPSILON);
    }

    @Test
    public void testGetNiceBid() {
        ImmutableList<Bid> bidOptions
                = biddingStrategy.bidSpace.getBids(BigDecimal.valueOf(0.5));
        Map<PartyId, OpponentModel> opponentModels = new HashMap<>();
        Group42OpponentModel om = mock(Group42OpponentModel.class);
        when(om.getUtility(any())).thenReturn(BigDecimal.valueOf(0.5))
                .thenReturn(BigDecimal.valueOf(0.1))
                .thenReturn(BigDecimal.valueOf(0.01));
        opponentModels.put(new PartyId("party2"), om);
        when(boaState.getOpponentModels()).thenReturn(opponentModels);

        assertEquals(bidOptions.get(0), biddingStrategy.getNiceBid(bidOptions, boaState));
    }

}
