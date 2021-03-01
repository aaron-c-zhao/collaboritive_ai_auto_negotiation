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
import tudelft.utilities.logging.Reporter;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Group42TicForTacTest {

    private static final double EPSILON = 1e-4;
    private static final PartyId PARTY1 = new PartyId("party1");
    private static final String PROFILE = "src/test/resources/testprofile.json";
    private static final String SAOP = "SAOP";
    private final ProtocolRef protocol = new ProtocolRef(SAOP);
    private final Parameters parameters = new Parameters();
    private LinearAdditive profile;
    private final static ObjectMapper jackson = new ObjectMapper();

    private Group42TicForTac biddingStrategy;
    @Mock
    private BoaState boaState;
    private Settings settings;
    @Mock
    private ProgressRounds progress;
    @Mock
    private Reporter reporter;
    @Mock
    private Group42OpponentModel opponentModel;


    @Before
    public void setup() throws URISyntaxException, IOException {
        biddingStrategy = spy(new Group42TicForTac());
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

        Map<PartyId, OpponentModel> opponentModels = new HashMap<>();
        opponentModels.put(new PartyId("party2"), opponentModel);
        when(boaState.getOpponentModels()).thenReturn(opponentModels);
    }


    @Test
    public void testProgressFirstPhase() {
        double pro = 0.05;
        Group42BiddingStrategy bsInit = new Group42BiddingStrategy();
        when(progress.get(any())).thenReturn(pro);
        when(opponentModel.getUtility(any())).thenReturn(BigDecimal.valueOf(0.5));
        Action action = bsInit.getAction(boaState);


        assertEquals((1 - pro / 10) * bsInit.getMax(), bsInit.getTargetUtility(0.05), EPSILON);
    }

    @Test
    public void testUpdateNashPoint() {
        doReturn(0.5).when(biddingStrategy).getOpponentUtility(any(), any());
        biddingStrategy.updateNashPoint(boaState);
        assertEquals(biddingStrategy.getMax()
                , biddingStrategy.getNashPoint()[0], EPSILON);
    }

    @Test
    public void testTargetUtilityPhaseTwo() {
        doReturn(new double[]{0.5, 0.5}).when(biddingStrategy).getNashPoint();
        doReturn(0.4).when(biddingStrategy).getOpponentUtility(any(), any());
        double progress = 0.5;
        Bid bid = mock(Bid.class);

        assertEquals(1.0 - 0.8* (1.0 - 0.5)
                , biddingStrategy.getTargetUtility(progress, bid, boaState)
                , EPSILON);
    }




}
