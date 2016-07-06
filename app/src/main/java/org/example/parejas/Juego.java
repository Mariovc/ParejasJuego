package org.example.parejas;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchUpdateReceivedListener;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.games.snapshot.Snapshots;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Author: Mario Velasco Casquero
 * Date: 05/07/2016
 * Email: m3ario@gmail.com
 */
public class Juego extends Activity implements
        OnTurnBasedMatchUpdateReceivedListener,
        RoomStatusUpdateListener,
        RoomUpdateListener,
        RealTimeMessageReceivedListener {
    private Drawable imagenOculta;
    private List<Drawable> imagenes;
    private Casilla primeraCasilla;
    private Casilla segundaCasilla;
    private ButtonListener botonListener;
    private TableLayout tabla;
    private actualizaCasillas handler;
    private Context context;
    private static Object lock = new Object();
    private Button[][] botones;
    private ButtonListener btnCasilla_Click;
    private static final int RC_SAVED_GAMES = 9009;
    String PartidaGuardadaNombre;
    private byte[] datosPartidaGuardada;
    String mRoomId = null;
    ArrayList<Participant> mParticipants = null;
    String mMyId = null;
    final static int RC_WAITING_ROOM = 10002;
    int jugadorLocal = 1;
    final static int RC_LOOK_AT_MATCHES = 10001;
    private AlertDialog mDialogoAlerta;
    public TurnBasedMatch mMatch;
    private Turno mTurnData;
    private int turnoPartidaPorTurnos;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new actualizaCasillas();
        cargarImagenes();
        setContentView(R.layout.juego);
        imagenOculta = getResources().getDrawable(R.drawable.icon);
        tabla = (TableLayout) findViewById(R.id.TableLayoutCasilla);
        context = tabla.getContext();
        btnCasilla_Click = new ButtonListener();
        switch (Partida.tipoPartida) {
            case "LOCAL":
                mostrarTablero();
                break;
            case "GUARDADA":
                mostrarPartidasGuardadas();
                break;
            case "REAL":
                iniciarPartidaEnTiempoReal();
                break;
            case "TURNO":
                iniciarPartidaPorTurnos();
                break;
        }
    }

    private void mostrarPartidasGuardadas() {
        int maxNumberOfSavedGamesToShow = 5;
        Intent savedGamesIntent =
                Games.Snapshots.getSelectSnapshotIntent(Partida.mGoogleApiClient,
                        "Partidas guardadas", true, true, maxNumberOfSavedGamesToShow);
        startActivityForResult(savedGamesIntent, RC_SAVED_GAMES);
    }

    class actualizaCasillas extends Handler {
        @Override
        public void handleMessage(Message msg) {
            synchronized (lock) {
                compruebaCasillas();
            }
        }

        public void compruebaCasillas() {
            if (Partida.casillas[segundaCasilla.x][segundaCasilla.y] ==
                    Partida.casillas[primeraCasilla.x][primeraCasilla.y]) {
                //ACIERTO
                Partida.casillas[segundaCasilla.x][segundaCasilla.y] = 0;
                Partida.casillas[primeraCasilla.x][primeraCasilla.y] = 0;
                botones[primeraCasilla.x][primeraCasilla.y].setVisibility(View.INVISIBLE);
                botones[segundaCasilla.x][segundaCasilla.y].setVisibility(View.INVISIBLE);
                if (Partida.turno == 1) {
                    Partida.puntosJ1 += 2;
                } else {
                    Partida.puntosJ2 += 2;
                }
                if ((Partida.puntosJ1 + Partida.puntosJ2) ==
                        (Partida.FILAS * Partida.COLUMNAS)) {
                    //FIN JUEGO
                    if (Partida.tipoPartida=="REAL"){
                        int puntos;
                        if (jugadorLocal==1){
                            puntos=Partida.puntosJ1;
                        } else {
                            puntos = Partida.puntosJ2;
                        }
                        Games.Leaderboards.submitScore(Partida.mGoogleApiClient,
                                getString(R.string. marcador_tiempoReal_id) ,puntos );
                    }
                    ((TextView) findViewById(R.id.jugador)).setText("GANADOR JUGADOR " +
                            (Partida.turno) + "");
                    if (Partida.tipoPartida=="TURNO"){
                        mTurnData.puntosJ1=Partida.puntosJ1;
                        mTurnData.puntosJ2=Partida.puntosJ2;
                        mTurnData.turnoJugador=Partida.turno;
                        mTurnData.casillas=Partida.casillas;
                        Games.TurnBasedMultiplayer.finishMatch(Partida.mGoogleApiClient,
                                mMatch.getMatchId());
                        Toast.makeText(getApplicationContext(), "Fin de la partida.",
                                Toast.LENGTH_LONG).show();
                        mTurnData = null;
                    }
                }
            } else {
                //FALLO
                segundaCasilla.boton.setBackgroundDrawable(imagenOculta);
                primeraCasilla.boton.setBackgroundDrawable(imagenOculta);
                if (Partida.turno == 1) {
                    Partida.turno = 2;
                } else {
                    Partida.turno = 1;
                }
                if (Partida.tipoPartida=="TURNO"){
                    mTurnData.puntosJ1=Partida.puntosJ1;
                    mTurnData.puntosJ2=Partida.puntosJ2;
                    mTurnData.turnoJugador=Partida.turno;
                    mTurnData.casillas=Partida.casillas;
                    String nextParticipantId =dameIdSiguienteJugador();
                    Games.TurnBasedMultiplayer.takeTurn(Partida.mGoogleApiClient,
                            mMatch.getMatchId(),
                            mTurnData.persist(), nextParticipantId).setResultCallback(
                            new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                                @Override
                                public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                                }
                            });
                    Toast.makeText(getApplicationContext(), "Fin de tu turno.",
                            Toast.LENGTH_LONG).show();
                    mTurnData = null;
                }
            }
            primeraCasilla = null;
            segundaCasilla = null;
        }
    }


    private void cargarImagenes() {
        imagenes = new ArrayList<Drawable>();
        imagenes.add(getResources().getDrawable(R.drawable.card1));
        imagenes.add(getResources().getDrawable(R.drawable.card2));
        imagenes.add(getResources().getDrawable(R.drawable.card3));
        imagenes.add(getResources().getDrawable(R.drawable.card4));
        imagenes.add(getResources().getDrawable(R.drawable.card5));
        imagenes.add(getResources().getDrawable(R.drawable.card6));
        imagenes.add(getResources().getDrawable(R.drawable.card7));
        imagenes.add(getResources().getDrawable(R.drawable.card8));
        imagenes.add(getResources().getDrawable(R.drawable.card9));
        imagenes.add(getResources().getDrawable(R.drawable.card10));
        imagenes.add(getResources().getDrawable(R.drawable.card11));
        imagenes.add(getResources().getDrawable(R.drawable.card12));
        imagenes.add(getResources().getDrawable(R.drawable.card13));
        imagenes.add(getResources().getDrawable(R.drawable.card14));
        imagenes.add(getResources().getDrawable(R.drawable.card15));
        imagenes.add(getResources().getDrawable(R.drawable.card16));
        imagenes.add(getResources().getDrawable(R.drawable.card17));
        imagenes.add(getResources().getDrawable(R.drawable.card18));
        imagenes.add(getResources().getDrawable(R.drawable.card19));
        imagenes.add(getResources().getDrawable(R.drawable.card20));
        imagenes.add(getResources().getDrawable(R.drawable.card21));
    }

    class ButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            synchronized (lock) {
                if (Partida.tipoPartida=="REAL"){
                    if (Partida.turno!=jugadorLocal){
                        Toast.makeText(getApplicationContext(), "No es tu turno.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                if(primeraCasilla!=null && segundaCasilla != null){
                    return;
                }
                int id = v.getId();
                int x = id/100;
                int y = id%100;
                descubrirCasilla(x, y);
                if (Partida.tipoPartida=="REAL"){
                    byte[] mensaje;
                    mensaje = new byte[3];
                    mensaje[0] = (byte) 'C';
                    mensaje[1] = (byte) x;
                    mensaje[2] = (byte) y;
                    for (Participant p : mParticipants) {
                        if (!p.getParticipantId().equals(mMyId)) {
                            Games.RealTimeMultiplayer.sendReliableMessage(
                                    Partida.mGoogleApiClient,
                                    null, mensaje,
                                    mRoomId, p.getParticipantId());
                        }
                    }
                }
            }
        }
    }

    private void descubrirCasilla(int x, int y) {
        Button button = botones[x][y];
        button.setBackgroundDrawable(imagenes.get(Partida.casillas[x][y]));
        if (primeraCasilla == null) {
            primeraCasilla = new Casilla(button, x, y);
        } else {
            if (primeraCasilla.x == x && primeraCasilla.y == y) {
                return;
            }
            segundaCasilla = new Casilla(button, x, y);
            ((TextView) findViewById(R.id.marcador)).setText("JUGADOR 1= "
                    + (Partida.puntosJ1) + " : JUGADOR 2= " + (Partida.puntosJ2));
            ((TextView) findViewById(R.id.jugador)).setText("TURNO JUGADOR "
                    + (Partida.turno) + "");
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    try {
                        synchronized (lock) {
                            handler.sendEmptyMessage(0);
                        }
                    } catch (Exception e) {
                        Log.e("E1", e.getMessage());
                    }
                }
            };
            Timer t = new Timer(false);
            t.schedule(tt, 1300);
        }
    }

    private void mostrarTablero() {
        botones = new Button[Partida.COLUMNAS][Partida.FILAS];
        for (int y = 0; y < Partida.FILAS; y++) {
            tabla.addView(crearFila(y));
        }
        ((TextView) findViewById(R.id.marcador)).setText("JUGADOR 1= "
                + (Partida.puntosJ1) + " : JUGADOR 2= " + (Partida.puntosJ2));
        ((TextView) findViewById(R.id.jugador)).setText("TURNO JUGADOR "
                + (Partida.turno) + "");
    }

    private TableRow crearFila(int y) {
        TableRow row = new TableRow(context);
        row.setHorizontalGravity(Gravity.CENTER);
        for (int x = 0; x < Partida.COLUMNAS; x++) {
            row.addView(crearCasilla(x, y));
            if (Partida.casillas[x][y] == 0) {
                botones[x][y].setVisibility(View.INVISIBLE);
            }
        }
        return row;
    }

    private View crearCasilla(int x, int y) {
        Button button = new Button(context);
        button.setBackgroundDrawable(imagenOculta);
        button.setId(100 * x + y);
        button.setOnClickListener(btnCasilla_Click);
        botones[x][y] = button;
        return button;
    }

        @Override
    public void onActivityResult(int requestCode, int responseCode, Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);
        switch (requestCode) {
            case RC_SAVED_GAMES:
                if (intent != null) {
                    if (intent.hasExtra(Snapshots.EXTRA_SNAPSHOT_METADATA)) {
                        SnapshotMetadata snapshotMetadata = (SnapshotMetadata)
                                intent.getParcelableExtra(Snapshots.EXTRA_SNAPSHOT_METADATA);
                        PartidaGuardadaNombre = snapshotMetadata.getUniqueName();
                        cargarSnapshotPartidaGuardada();
                        return;
                    } else if (intent.hasExtra(Snapshots.EXTRA_SNAPSHOT_NEW)) {
                        nuevoSnapshotPartidaGuadada();
                    }
                } else {
                    finish();
                }
                break;
            case RC_WAITING_ROOM:
                if (responseCode == Activity.RESULT_OK) {
                    numeroJugadorLocal();
                    enviarTableroOponentes();
                    mostrarTablero();
                } else {
                    finish();
                }
                break;
            case RC_LOOK_AT_MATCHES:
                if (responseCode != Activity.RESULT_OK) {
                    return;
                }
                TurnBasedMatch match = intent
                        .getParcelableExtra(Multiplayer.EXTRA_TURN_BASED_MATCH);
                if (match != null) {
                    gestionarPartidaTurno(match);
                }
                break;
        }
        super.onActivityResult(requestCode, responseCode, intent);
    }

    void codificaPartidaGuardada() {
        datosPartidaGuardada = new byte[Partida.FILAS * Partida.COLUMNAS];
        int k = 0;
        for (int i = 0; i < Partida.FILAS; i++) {
            for (int j = 0; j < Partida.COLUMNAS; j++) {
                datosPartidaGuardada[k] = (byte) Partida.casillas[i][j];
                k++;
            }
        }
    }

    void decodificaPartidaGuardada() {
        int i = 0;
        int j = 0;
        for (int k = 0; k < Partida.FILAS * Partida.COLUMNAS; k++) {
            Partida.casillas[i][j] = (int) datosPartidaGuardada[k];
            if (j < Partida.COLUMNAS - 1) {
                j++;
            } else {
                j = 0;
                if (i < Partida.FILAS - 1) {
                    i++;
                } else {
                    i = 0;
                }
            }
        }
    }

    void nuevoSnapshotPartidaGuadada() {
        AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                String unique = new BigInteger(281, new Random()).toString(13);
                PartidaGuardadaNombre = "Parejas-" + unique;
                Snapshots.OpenSnapshotResult open = Games.Snapshots.open(
                        Partida.mGoogleApiClient, PartidaGuardadaNombre, true).await();
                if (!open.getStatus().isSuccess()) {
                    return 0;
                }
                codificaPartidaGuardada();
                Snapshot snapshot = open.getSnapshot();
                snapshot.getSnapshotContents().writeBytes(datosPartidaGuardada);
                Date d = new Date();
                SnapshotMetadataChange metadataChange =
                        new SnapshotMetadataChange.Builder()
                                .fromMetadata(snapshot.getMetadata())
                                .setDescription("Parejas " + DateFormat.format("yyyy.MM.dd",
                                        d.getTime()).toString())
                                .build();
                Snapshots.CommitSnapshotResult commit =
                        Games.Snapshots.commitAndClose(
                                Partida.mGoogleApiClient, snapshot, metadataChange).await();
                return -1;
            }

            @Override
            protected void onPostExecute(Integer status) {
                if (status == -1) {
                    mostrarTablero();
                }
            }
        };
        task.execute();
    }

    @Override
    public void onBackPressed() {
        if (Partida.tipoPartida == "GUARDADA") {
            guardarPartidaGuardada();
        }
        Juego.this.finish();
    }

    public void guardarPartidaGuardada() {
        codificaPartidaGuardada();
        AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                Snapshots.OpenSnapshotResult open = Games.Snapshots.open(
                        Partida.mGoogleApiClient, PartidaGuardadaNombre, false).await();
                if (open.getStatus().isSuccess()) {
                    Snapshot snapshot = open.getSnapshot();
                    guardarSnapshotPartidaGuardada(snapshot, datosPartidaGuardada,
                            "Partida de Parejas");
                    return 1;
                }
                return 0;
            }

            @Override
            protected void onPostExecute(Integer status) {
            }
        };
        task.execute();
    }

    private PendingResult<Snapshots.CommitSnapshotResult>
    guardarSnapshotPartidaGuardada(Snapshot snapshot, byte[] data, String desc) {
        snapshot.getSnapshotContents().writeBytes(data);
        SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                .setDescription(desc)
                .build();
        return Games.Snapshots.commitAndClose(Partida.mGoogleApiClient,
                snapshot, metadataChange);
    }
    void cargarSnapshotPartidaGuardada() {
        AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                Snapshots.OpenSnapshotResult result =
                        Games.Snapshots.open(Partida.mGoogleApiClient,
                                PartidaGuardadaNombre, true).await();
                if (result.getStatus().isSuccess()) {
                    Snapshot snapshot = result.getSnapshot();
                    try {
                        datosPartidaGuardada = new byte[0];
                        datosPartidaGuardada = snapshot.getSnapshotContents().readFully();
                    } catch (IOException e) {
                    }
                }
                return result.getStatus().getStatusCode();
            }
            @Override
            protected void onPostExecute(Integer status) {
                decodificaPartidaGuardada();
                mostrarTablero();
            }
        };
        task.execute();
    }
    @Override
    public void onPeerDeclined(Room room, List<String> arg1) {
        actualizaRoom(room);
    }
    @Override
    public void onP2PDisconnected(String participant) {
    }
    @Override
    public void onPeerJoined(Room room, List<String> arg1) {
        actualizaRoom(room);
    }
    @Override
    public void onRoomAutoMatching(Room room) {
        actualizaRoom(room);
    }
    @Override
    public void onPeersConnected(Room room, List<String> peers) {
        actualizaRoom(room);
    }
    @Override
    public void onRoomConnecting(Room room) {
        actualizaRoom(room);
    }
    @Override
    public void onPeerLeft(Room room, List<String> peersWhoLeft) {
        actualizaRoom(room);
    }
    @Override
    public void onDisconnectedFromRoom(Room room) {
        mRoomId = null;
        mostrarErrorJuego();
    }
    @Override
    public void onPeersDisconnected(Room room, List<String> peers) {
        actualizaRoom(room);
    }
    @Override
    public void onConnectedToRoom(Room room) {
        mParticipants = room.getParticipants();
        mMyId = room.getParticipantId(Games.Players.getCurrentPlayerId(
                Partida.mGoogleApiClient));
        if(mRoomId==null)
            mRoomId = room.getRoomId();
    }
    @Override
    public void onPeerInvitedToRoom(Room room, List<String> arg1) {
        actualizaRoom(room);
    }
    @Override
    public void onP2PConnected(String participant) {
    }
    @Override
    public void onRoomCreated(int statusCode, Room room) {
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            mostrarErrorJuego();
            return;
        }
        mRoomId = room.getRoomId();
        mostrarEsperandoARoom(room);
    }
    @Override
    public void onLeftRoom(int statusCode, String roomId) {
    }
    @Override
    public void onJoinedRoom(int statusCode, Room room) {
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            mostrarErrorJuego();
            return;
        }
        mostrarEsperandoARoom(room);
    }
    @Override
    public void onRoomConnected(int statusCode, Room room) {
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            mostrarErrorJuego();
            return;
        }
        actualizaRoom(room);
    }

        @Override
    public void onRealTimeMessageReceived(RealTimeMessage rtm) {
        byte[] buf = rtm.getMessageData();
        String sender = rtm.getSenderParticipantId();
        if (buf[0] == 'A'){
            int x =  buf[1];
            int y = buf[2];
            int valor = buf[3];
            Partida.casillas[x][y]= valor;
        }
        if (buf[0] == 'C'){
            int x =  buf[1];
            int y = buf[2];
            descubrirCasilla(x, y);
        }
    }
    private void iniciarPartidaEnTiempoReal() {
        final int NUMERO_MINIMO_OPONENTES = 1, NUMERO_MAXIMO_OPONENTES = 1;
        Bundle criterioPartidaRapida =
                RoomConfig.createAutoMatchCriteria(NUMERO_MINIMO_OPONENTES,
                        NUMERO_MAXIMO_OPONENTES, 0);
        RoomConfig.Builder roomConfiguradorConstructor = RoomConfig.builder(this);
        roomConfiguradorConstructor.setMessageReceivedListener(this);
        roomConfiguradorConstructor.setRoomStatusUpdateListener(this);
        roomConfiguradorConstructor.setAutoMatchCriteria(criterioPartidaRapida);
        Games.RealTimeMultiplayer.create(Partida.mGoogleApiClient,
                roomConfiguradorConstructor.build());
    }
    private void numeroJugadorLocal() {
        jugadorLocal = 1;
        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId))
                continue;
            if (p.getStatus() != Participant.STATUS_JOINED)
                continue;
            if (p.getParticipantId().compareTo(mMyId) < 0)
                jugadorLocal = 2;
        }
    }
    public void enviarTableroOponentes() {
        if (jugadorLocal == 1) {
            for (int fila = 0; fila < Partida.FILAS; fila++) {
                for (int columna = 0; columna < Partida.COLUMNAS; columna++) {
                    byte[] mensaje;
                    mensaje = new byte[4];
                    mensaje[0] = (byte) 'A';
                    mensaje[1] = (byte) fila;
                    mensaje[2] = (byte) columna;
                    mensaje[3] = (byte) Partida.casillas[fila][columna];
                    for (Participant p : mParticipants) {
                        if (!p.getParticipantId().equals(mMyId)) {
                            Games.RealTimeMultiplayer.sendReliableMessage(
                                    Partida.mGoogleApiClient, null, mensaje,
                                    mRoomId, p.getParticipantId());
                        }
                    }
                }
            }
        }
    }
    void actualizaRoom(Room room) {
        if (room != null) {
            mParticipants = room.getParticipants();
        }
    }
    void mostrarErrorJuego() {
        BaseGameUtils.makeSimpleDialog(this, "Oops! Ha ocurrido un error.");
        finish();
    }
    void mostrarEsperandoARoom(Room room) {
        final int MIN_PLAYERS = Integer.MAX_VALUE;
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(
                Partida.mGoogleApiClient, room, MIN_PLAYERS);
        startActivityForResult(i, RC_WAITING_ROOM);
    }
    @Override
    public void onTurnBasedMatchReceived(TurnBasedMatch match) {
    }
    @Override
    public void onTurnBasedMatchRemoved(String matchId) {
    }
    public void iniciarPartidaPorTurnos(){
        Intent intent = Games.TurnBasedMultiplayer
                .getInboxIntent(Partida.mGoogleApiClient);
        startActivityForResult(intent, RC_LOOK_AT_MATCHES);
    }
    public void gestionarPartidaTurno(TurnBasedMatch match) {
        mMatch = match;
        int status = match.getStatus();
        int turnStatus = match.getTurnStatus();
        switch (status) {
            case TurnBasedMatch.MATCH_STATUS_CANCELED:
                mostrarAdvertencia("Cancelada!", "Este partida ha sido cancelada!");
                return;
            case TurnBasedMatch.MATCH_STATUS_EXPIRED:
                mostrarAdvertencia("Expirada!", "Esta partida ha expirado!");
                return;
            case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
                mostrarAdvertencia("Esperando a jugadores aleatorios...",
                        "Todavía estamos esperando a jugadores aleatorios.");
                return;
            case TurnBasedMatch.MATCH_STATUS_COMPLETE:
                if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE) {
                    mostrarAdvertencia( "Completada!",
                            "Esta partida ha finalizado! No hay nada que hacer");
                    break;
                }
                mostrarAdvertencia("Complete!",
                        "Esta partida ha finalizado!  Solamente puedes finalizarla.");
        }
        switch (turnStatus) {
            case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
                mTurnData = Turno.unpersist(mMatch.getData());
                if (match.getData() == null) {
                    inicializarPartidaPorTurnos(mMatch);
                }
                mostrarPartidaPorTurnos(mMatch);
                return;
            case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
                mostrarAdvertencia("Turno...", "No es tu turno.");
                break;
            case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
                mostrarAdvertencia("Esperando!",
                        "Esperando a que respondan a las invitaciones!");
        }
        mTurnData = null;
    }
    public void mostrarAdvertencia(String title, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(title).setMessage(message);
        alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        mDialogoAlerta = alertDialogBuilder.create();
        mDialogoAlerta.show();
    }
    public void inicializarPartidaPorTurnos(TurnBasedMatch match) {
        mTurnData = new Turno();
        mTurnData.nivel = 1;
        mTurnData.filas=4;
        mTurnData.columnas=4;
        mTurnData.casillas = new int [mTurnData.columnas] [mTurnData.filas];
        mTurnData.puntosJ1=0;
        mTurnData.puntosJ2=0;
        mTurnData.turnoJugador=1;
        try{
            int size = mTurnData.filas*mTurnData.columnas;
            ArrayList<Integer> list = new ArrayList<Integer>();
            for(int j=0;j<size;j++){
                list.add(new Integer(j));
            }
            Random r = new Random();
            for(int i=size-1;i>=0;i--){
                int t = 0;
                if(i>0){
                    t = r.nextInt(i);
                }
                t=list.remove(t).intValue();
                mTurnData.casillas[i%mTurnData.filas][i/mTurnData.columnas]=1+(t%(size/2));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        mMatch = match;
        String playerId = Games.Players.getCurrentPlayerId(Partida.mGoogleApiClient);
        String myParticipantId = mMatch.getParticipantId(playerId);
        Games.TurnBasedMultiplayer.takeTurn(Partida.mGoogleApiClient, match.getMatchId(),
                mTurnData.persist(), myParticipantId).setResultCallback(
                new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                    }
                });
    }
    public void mostrarPartidaPorTurnos(TurnBasedMatch match) {
        mTurnData.unpersist(match.getData());
        Partida.FILAS=mTurnData.filas;
        Partida.COLUMNAS=mTurnData.columnas;
        Partida.puntosJ1= mTurnData.puntosJ1;
        Partida.puntosJ2=mTurnData.puntosJ2;
        Partida.turno=mTurnData.turnoJugador;
        turnoPartidaPorTurnos=mTurnData.turnoJugador;
        Partida.casillas = new int [mTurnData.columnas][mTurnData.filas];
        Partida.casillas=mTurnData.casillas;
        mostrarTablero();
    }
    public String dameIdSiguienteJugador() {
        String playerId = Games.Players.getCurrentPlayerId(Partida.mGoogleApiClient);
        String myParticipantId = mMatch.getParticipantId(playerId);
        ArrayList<String> participantIds = mMatch.getParticipantIds();
        int desiredIndex = -1;
        for (int i = 0; i < participantIds.size(); i++) {
            if (participantIds.get(i).equals(myParticipantId)) {
                desiredIndex = i + 1;
            }
        }
        if (desiredIndex < participantIds.size()) {
            return participantIds.get(desiredIndex);
        }
        if (mMatch.getAvailableAutoMatchSlots() <= 0) {
            return participantIds.get(0);
        } else {
            return null;
        }
    }
}