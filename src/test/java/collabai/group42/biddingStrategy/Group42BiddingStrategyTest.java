package collabai.group42.biddingStrategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import geniusweb.actions.Action;
import geniusweb.actions.PartyId;
import geniusweb.boa.BoaState;
import geniusweb.inform.Settings;
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
import tudelft.utilities.logging.Reporter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class Group42BiddingStrategyTest {

    private static final double EPSILON = 1e10 - 4;
    private static final PartyId PARTY1 = new PartyId("party1");
    private static final String PROFILE = "src/test/resources/testprofile.json";
    private static final String SAOP = "SAOP";
    private final ProtocolRef protocol = new ProtocolRef(SAOP);
    private final Parameters parameters = new Parameters();
    private LinearAdditive profile;
    private final static ObjectMapper jackson = new ObjectMapper();

    private Group42BiddingStrategy biddingStrategy;
    @Mock
    private Group42BiddingStrategy mockBiddingStrategy;
    @Mock
    private BoaState boaState;
    private Settings settings;
    @Mock
    private ProgressRounds progress;
    @Mock
    private Reporter reporter;


    @Before
    public void setup() throws URISyntaxException, IOException {
        biddingStrategy = new Group42BiddingStrategy();
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

        assertEquals(0.95, bsInit.getTargetUtility(0.05), EPSILON);
    }

    @Test
    public void testTargetUtilitySecondPhaseToughOpponent() {
        when(mockBiddingStrategy.getNiceness()).thenReturn(0.0);
        when(mockBiddingStrategy.getTargetUtility(any())).thenCallRealMethod();
        assertEquals(0.9 - Math.pow((0.5 - 0.05), 3.0),
                mockBiddingStrategy.getTargetUtility(0.5),
                EPSILON);
    }

    @Test
    public void testTargetUtilitySecondPhaseNiceOpponent() {
        when(mockBiddingStrategy.getNiceness()).thenReturn(0.3);
        when(mockBiddingStrategy.getTargetUtility(any())).thenCallRealMethod();
        assertEquals(0.9 - Math.pow((0.5 - (0.05 + 0.2 * 0.3)), 3.0),
                mockBiddingStrategy.getTargetUtility(0.5),
                EPSILON);
    }

    @Test
    public void testTargetUtilityThirdPhaseToughOpponent() {
        when(mockBiddingStrategy.getNiceness()).thenReturn(0.0);
        when(mockBiddingStrategy.getTargetUtility(any())).thenCallRealMethod();
        assertEquals(0.9 - Math.pow((0.5 - 0.05), 5.0),
                mockBiddingStrategy.getTargetUtility(0.7),
                EPSILON);
    }

    @Test
    public void testTargetUtilityThirdPhaseNiceOpponent() {
        when(mockBiddingStrategy.getNiceness()).thenReturn(0.4);
        when(mockBiddingStrategy.getTargetUtility(any())).thenCallRealMethod();
        assertEquals(0.9 - Math.pow((0.5 - 0.05), 2.0 + 3.0 * 0.4),
                mockBiddingStrategy.getTargetUtility(0.7),
                EPSILON);
    }

    @Test
    public void testGetMin() {
        assertEquals(biddingStrategy.getMin(), 0.24, EPSILON);
    }

    @Test
    public void testGetUtility() {
        assertEquals(biddingStrategy.getUtility(biddingStrategy.getBidSpace().getRvBid()), 0.24, EPSILON);
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
            mockList1.add(new ArrayList<>(Arrays.asList(1 - i * 0.01, 0.2 + i * 0.01)));
            mockList2.add(new ArrayList<>(Arrays.asList(1 - i * 0.01, 0.2)));
        }

        when(mockBiddingStrategy.getNiceness()).thenCallRealMethod();

        when(mockBiddingStrategy.getRecentBids()).thenReturn(mockList1);
        assertEquals(Math.PI / 4, mockBiddingStrategy.getNiceness(), EPSILON);

        when(mockBiddingStrategy.getRecentBids()).thenReturn(mockList2);
        assertEquals(0.0, mockBiddingStrategy.getNiceness(), EPSILON);
    }

    @Test
    public void testSetBAndGetB() {
        when(mockBiddingStrategy.getNiceness()).thenReturn(0.2);
        mockBiddingStrategy.setB();
        assertEquals(0.05 + 0.2 * mockBiddingStrategy.getNiceness(), mockBiddingStrategy.getB(), EPSILON);
    }

    @Test
    public void testSetAAndGetA() {
        when(mockBiddingStrategy.getNiceness()).thenReturn(0.2);
        mockBiddingStrategy.setA();
        assertEquals(2.0 + 3.0 * mockBiddingStrategy.getNiceness(), mockBiddingStrategy.getA(), EPSILON);
    }


}
