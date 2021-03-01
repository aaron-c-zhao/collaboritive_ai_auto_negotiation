package collabai.group42.biddingStrategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import geniusweb.actions.Action;
import geniusweb.actions.PartyId;
import collabai.group42.BoaState;
import geniusweb.inform.Settings;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.Profile;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.progress.ProgressRounds;
import geniusweb.references.Parameters;
import geniusweb.references.ProfileRef;
import geniusweb.references.ProtocolRef;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.Invocation;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import tudelft.utilities.logging.Reporter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Group42TicForTacTest {

    private static final double EPSILON = 1e10 - 4;
    private static final PartyId PARTY1 = new PartyId("party1");
    private static final String PROFILE = "src/test/resources/testprofile.json";
    private static final String SAOP = "SAOP";
    private final ProtocolRef protocol = new ProtocolRef(SAOP);
    private final Parameters parameters = new Parameters();
    private LinearAdditive profile;
    private final static ObjectMapper jackson = new ObjectMapper();

    private Group42TicForTac biddingStrategy;
    @Mock
    private Group42TicForTac mockBiddingStrategy;
    @Mock
    private BoaState boaState;
    private Settings settings;
    @Mock
    private ProgressRounds progress;
    @Mock
    private Reporter reporter;
    @Captor
    private ArgumentCaptor<Bid> bidCaptor;


    @Before
    public void setup() throws URISyntaxException, IOException {
        biddingStrategy = new Group42TicForTac();
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











}
