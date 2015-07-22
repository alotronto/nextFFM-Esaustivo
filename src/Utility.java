import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;


/**
 * Classe di utilia'
 * 
 * @author Andrea Rocco Lotronto 01-08-2013
 * 
 */
public class Utility {

	
	//Attributi della classe per eseguire stampe su file
	static private FileWriter myWriter;
    static private BufferedWriter myBufferWriter;
	
    //Attributi della classe per la memorizzazione delle squadre, interventi
	public static ArrayList<Intervento> interventi = new ArrayList<Intervento>();
	public static ArrayList<Squadra> squadre = new ArrayList<Squadra>();
	
	//Attributi per il calcolo del costo delle soluzioni
	public static ArrayList<Map<String, String>> distanzeImpianti = new ArrayList<Map<String, String>>();
	private static float costoBenzina;
	private static float costoDiesel;
	private static String idSedediPartenza="0";
	
	private static Statement myStatement;
	private static ResultSet myResultSet;
	private static Connection myConnection;

	// ----Parametri di connessione db-------
	private static String host = "";
	private static String db = "";
	private static String user = "";
	private static String pass = "";

	
	
	
	/**
	 * Metodo per apertura di una connessione ad un DBMS
	 * 
	 * @param host
	 * @param db
	 * @param user
	 * @param pass
	 */
	public static void dbOpenConnection(String host, String db, String user,String pass) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			String connectionUrl = "jdbc:mysql://" + host + ":3306/" + db
					+ "?user=" + user + "&password=" + pass;
			myConnection = DriverManager.getConnection(connectionUrl);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.getMessage());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Metodo per la chiusura della connessione al DBMS
	 */
	public static void dbCloseConnection() {
		try {
			myResultSet.close();
			myStatement.close();
			myConnection.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.getMessage());
		}

	}

	
	/**
	 * Metdo per l'estrazione di  tutte le squadre dal DB;
	 * Per ogni Squadra esistente calcolo le competenze totali della squadra;
	 * Per ogni Squadra viene calcolato il costo orario
	 * Per ogni squadra inizializzo il tipo di automezzo ed il suo consumo.
	 * Vengono inoltre inizializzati (ALL'INTERNO DI QUESTO METODO PER COMODITA') il costo dei carburanti
	 * e una mappa con le distenze e i tempi di percorrenza tra le varie sedi 
	 */
	public static void initSquadre(){
		//Inizializzazione spostamenti
		initMapSpostamenti();
		
		//Inizializzazioni costi carburanti
		setCostoCarburanti();
		//Estrazione delle squadre da DB
		ResultSet myResutlSet = Utility.getSquadre();
		try {
			while (myResutlSet.next()) {
				squadre.add(new Squadra(myResutlSet.getString("idSquadra")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		//Competenze per ogni squadra
		for(int i=0;i<squadre.size();i++){
			myResutlSet = Utility.getCopetenzethatSquadra(squadre.get(i).getId());
			try{
				while(myResutlSet.next()){
					squadre.get(i).addCompentenza(myResutlSet.getString("idCompetenza"));
				}
			}catch (SQLException e) {
				e.printStackTrace();
				System.out.println(e.getMessage());
			}
		}
		
		//Costo orario squadra
		for(int i=0;i<squadre.size();i++){
			squadre.get(i).setCostoOrarioSquadra(getCostoOrarioSquadra(squadre.get(i).getId()));
		}
		
		//Inizializzo il tipo di automezzo ed il suo consumo e la sua usura
		try{
			myResutlSet = Utility.initAutomezzoSquadre();
			
			for(int i=0;(i<squadre.size() && myResutlSet.next());i++){
				squadre.get(i).setAlimentazioneAuto(myResutlSet.getString("alimentazione"));
				squadre.get(i).setConsumo(myResutlSet.getFloat("consumo"));
				squadre.get(i).setCostoProporzionale(myResutlSet.getFloat("costiProporzionali"));
			}
		}catch (SQLException e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		dbCloseConnection();
		
	}
	
	/**
	 * Metodo per l'estrazione di  tutti gli interventi dal DB e dell'inizializzazione dei valori di 
	 * durata e idImpianto dei singoli interventi
	 */
	public static void initInterventi(){
		ResultSet myResutlSet = Utility.getInterventi();
		try{
			while(myResutlSet.next()){
				interventi.add(new Intervento(myResutlSet.getString("idIntervento")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		
		myResutlSet = Utility.getInfoInterventi();
		try{
			for(int i=0; i<interventi.size() && myResutlSet.next(); i++){
				interventi.get(i).setDurata(myResutlSet.getInt("durata"));
				interventi.get(i).setIdImpianto(myResutlSet.getString("idImpianto"));	
			}
			
			//Competenze necessarie per ogni intervento
			for(int i=0;i<interventi.size();i++){
				myResutlSet = Utility.getCopetenzeIntervento(interventi.get(i).getId());
				while(myResutlSet.next()){
					interventi.get(i).setCompentenza(myResutlSet.getString("idCompetenza"));
				}
			}
				
		}catch (SQLException e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		dbCloseConnection();
	}
	
	
	/**
	 * Metdodo per il reperimento delle competenze necessarie per l'esuzione di un determinato intervento
	 * @param id ID dell'intervento
	 * @return ResultSet contenente il risultato della query e in particolare idIntevento,idInterventoTipo,tipoCompetenza,idCompetenza
	 */
	public static ResultSet getCopetenzeIntervento(String id){
		dbOpenConnection(host, db, user, pass);
		try {
			myStatement = myConnection.createStatement();
			/*System.out.print("SELECT intervento.idIntervento, " +
					"intervento.idInterventoTipo, competenzetecniche.tipoCompetenza, competenzetecniche.idCompetenza " +
					"FROM  `capacitarichieste` JOIN intervento ON " +
					"intervento.idInterventoTipo = capacitarichieste.idInterventoTipo "+
					"JOIN competenzetecniche ON capacitarichieste.idCompetenza = competenzetecniche.idCompetenza "+
					"WHERE intervento.idIntervento= "+id+" ;");*/
			return myResultSet = myStatement.executeQuery("SELECT intervento.idIntervento, " +
					"intervento.idInterventoTipo, competenzetecniche.tipoCompetenza, competenzetecniche.idCompetenza " +
					"FROM  `capacitarichieste` JOIN intervento ON " +
					"intervento.idInterventoTipo = capacitarichieste.idInterventoTipo "+
					"JOIN competenzetecniche ON capacitarichieste.idCompetenza = competenzetecniche.idCompetenza "+
					"WHERE intervento.idIntervento= "+id+" ;");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.getMessage());
			return myResultSet = null;
		}
	}
	
	/**
	 * Metodo per l'estrazione delle informazioni sulle squadre dal database
	 * 
	 * @return ResultSet della query "Select * from squadra"
	 * 
	 */
	public static ResultSet getSquadre() {
		dbOpenConnection(host, db, user, pass);
		try {
			myStatement = myConnection.createStatement();
			return myResultSet = myStatement
					.executeQuery("SELECT * FROM squadra");
			
		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.getMessage());
			return myResultSet = null;
		}
		
	}

	/**
	 * Metodo per l'estrazione delle informazioni sugli interventi dal database
	 * 
	 * @return ResultSet della query "Select * from intervento"
	 * 
	 */
	public static ResultSet getInterventi() {
		dbOpenConnection(host, db, user, pass);
		try {
			myStatement = myConnection.createStatement();
			return myResultSet = myStatement
					.executeQuery("SELECT * FROM intervento");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.getMessage());
			return myResultSet = null;
		}
	}
	
	
	/**
	 * Metodo per determinare tutte le competenze di una squadra
	 * @param id idSquadra 
	 * @return Resultset contenete una tabella con gli id delle competenze della squadra
	 */
	public static ResultSet getCopetenzethatSquadra(String id){
		dbOpenConnection(host, db, user, pass);
		try {
			myStatement = myConnection.createStatement();
			return myResultSet = myStatement
					.executeQuery("SELECT DISTINCT capacitaoperative.IdCompetenza"
							+ " FROM squadra JOIN risorsaoperativa ON "
							+ "squadra.idSquadra = risorsaoperativa.idSquadra JOIN "
							+ "capacitaoperative ON risorsaoperativa.idOperatore = "
							+ "capacitaoperative.IdOperatore "
							+ "WHERE squadra.idSquadra = "+id+";");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.getMessage());
			return myResultSet = null;
		}
	}
	
	
	
	
	
	/**
	 * Metodo per la determinazione della soluzione con minor costo.
	 * Il metdo dopo aver generato tutte le possibili distribuzioni degli eventi (NON considerando l'ordine degli interventi) 
	 * alle squadre.
	 * Determina per ogni ditribuzione la soluzione meno costosta CONSIDERANDO l'ordine di esecuzione degli interventi 
	 * 
	 * @param n numero di squadre
	 * @param m nnumero di interventi
	 * 
	 */
	public static void getAllSolution(int n, int m){
		
		//DEBUGG!!!!!!!!!!!!!
		//openFile("SoluzioniSin");
		//-----------------
		
		ArrayList<String> intermediatelSolution = new ArrayList<String>();
		ArrayList<OneSolution> totalSolutionNoOrdered = new ArrayList<OneSolution>();
		
		//Generazione della soluzione iniziale
		String init= new String();
		for(int i=0;i<m;i++){
			init+="0";
		}
		intermediatelSolution.add(new String(init));
		//Generazione di tutte le possibili soluzioni (disposizioni n m)
		//Generazioni in formato compatto di tutte le soluzioni di assegnazione degli interventi alle squadre
		for(int i=1; i<Math.pow(n, m); i++){
			intermediatelSolution.add(new String(Integer.toString((Integer.valueOf(intermediatelSolution.get(i-1), n)+1), n))); 
			if(intermediatelSolution.get(i).length()<m){
				String comodo = intermediatelSolution.get(i);
				for(int j=comodo.length();j<m;j++){
					intermediatelSolution.set(i, (comodo="0"+comodo));
				}
			}
		}
		
		//DEBUGGGGGG
		//printToFile("\nRappresentazioni sintentica della distribuzione degli interventi:\n");
		//for(String s : intermediatelSolution){
		//	printToFile(s+"\n");
		//}
		//closefile();
		//Generazione di tutte le possibili soluzioni in formato esteso
		//in particolare viene generata una matrice NxM dove la prsenza di 1 rappresenta
		//l'assegnazione del m-esimo intervento alla squadrea n-esima
		//Vengono generate solo tutte le possibili assegnazioni degli interventi alle Squadre
		//in questa situazione non vengono ancora considereta tutte le possibili PERMUTAZIONI sull'ordine degli interventi
		
		//DEBUGGGG
		//openFile("SoluzioniNoOrder");
		//printToFile("\nRappresentazioni della distribuzione degli interventi senza ordine::\n");
		
		for(int i=0;i<intermediatelSolution.size();i++){
			
			OneSolution comodoOneSolution = new OneSolution(n, m);
			String compactSolution = intermediatelSolution.get(i);
	
			for(int j=0;j<m;j++){
				
				int index = Character.getNumericValue(compactSolution.charAt(j));
				String comodocolum = getVector(index, n);
				comodoOneSolution.setColum(j, comodocolum);
				
			}
			
			
			totalSolutionNoOrdered.add(comodoOneSolution);
			//intermediatelSolution.remove(0);
			System.out.println("dim sintetic:"+intermediatelSolution.size());
			System.out.println("dim Extended_NO_ORDER:"+totalSolutionNoOrdered.size());
			
			//DEBUGGGGG
			//printToFile(comodoOneSolution.toString()+"\n");
		}
		//closefile();
		
		intermediatelSolution.clear();

		OneSolution oneSolutionNoOrder = new OneSolution(n, m);
		//Vengono generate tutte le possibili soluzioni in cui viene considerato anche l'ordine 
		//di esecuzione degli interventi e calcolato il costo
		
		//NECESSITA' DI MANTENERE UN VETTORE DEI COSTI CONTENENTI I MIGLIORI COSI PER SQUADRA
		float vetCostSquadre[] = new float[n]; 
		//NECESSITA' DI MANTENERE UN VETTORE DELLE CORRISPONDENTI LISTE DI ESECUZIONE DEGLI INTERVENTI
		ArrayList<String> vetExecSquadre = new ArrayList<String>();
		//NECESSITA' DI MATERE LA SOLUZIONE CON COSTO MINORE
		solutionInOrder minSolution = new solutionInOrder();
		//minSolution.setCostoTotale(0.0);
		
		//Elimino tutte le assegnazione di interventi a squadre senza competenze
		//Verifico che le squadre abbiano le competenze per gli interventi assegnati
		//MOMENTANEAMENTE IL CECK SULLE COMPOTENZE NON E' UTILE
		
		for(int i=0;i<totalSolutionNoOrdered.size();i++){
			System.out.println(i);
			oneSolutionNoOrder = totalSolutionNoOrdered.get(i);
			//solutionInOrder solutionComodo = new solutionInOrder();
			
			int[][] comodo = oneSolutionNoOrder.getMatrix();
			boolean controllo = true;
			for(int z=0; z<n && controllo; z++){
				String lista_interventi = new String();
				for(int w=0; w<m;w++){
					if(comodo[z][w]==1){
						//lista_interventi+=(w+1);
						lista_interventi+=(w);
					}
				}
				if(lista_interventi.length()>0){
					if(!checkListaInterventi(lista_interventi, String.valueOf(z))){
						controllo = false;
						totalSolutionNoOrdered.remove(i);
						i--;
						break;
					}
					else{
						System.out.println("Accetto");
					}
				}
				
			}
			
		}
		
		
		for(int i=0;i<totalSolutionNoOrdered.size();i++){
			
			System.out.println("Exam solution no order number :::"+i+"/"+totalSolutionNoOrdered.size());
			
			solutionInOrder minSolutionComodo = new solutionInOrder();
			oneSolutionNoOrder = totalSolutionNoOrdered.get(i);
				
			int[][] comodo = oneSolutionNoOrder.getMatrix();
			
			//Valutazione del costo per le tutte le possibile permutazioni
			//nell'ordine di esecuzione degli interventi associati
			for(int z=0; z<n; z++){
				//String lista_interventi = new String();
				ArrayList<Integer> lista_interventi = new ArrayList<Integer>();
				for(int w=0; w<m;w++){
					if(comodo[z][w]==1){
						//lista_interventi+=(w);
						lista_interventi.add(w+1);
					}
				}
	
				//Reperimento del costo totale per squadra
				ArrayList<String> listaCostMIN = new ArrayList<String>();
				//if(lista_interventi.length()>0){
				if(lista_interventi.size()>0){
					listaCostMIN = findMinCostOneNonOrder(lista_interventi, String.valueOf(z));
					vetExecSquadre.add(listaCostMIN.get(0));
					vetCostSquadre[z] = Float.valueOf(listaCostMIN.get(1));
				}
				else{
					vetExecSquadre.add("");
					vetCostSquadre[z]=0;
				}
			
			}
			
			//------------Necessita' di salvare la soluzione minore con ordinamento
			float costoTotale=0;
			for(int z=0;z<vetCostSquadre.length;z++){
				costoTotale+=vetCostSquadre[z];
			}
			
			
			//Salvataggio della soluzione con costo minimo
			if(i==0){
				minSolutionComodo.setCostoTotale(costoTotale);
				minSolutionComodo.setListaInterventi(vetExecSquadre);
				
				minSolution.setCostoTotale(minSolutionComodo.getCosto());
				minSolution.setListaInterventi(minSolutionComodo.getListaInterventi());
			}
			else{
				minSolutionComodo.setCostoTotale(costoTotale);
				minSolutionComodo.setListaInterventi(vetExecSquadre);
			}
			
			//DEBUGGGGGGGGGGGGGGGGGGGGg
			
			//printToFile("\n"+"soluzione valuata"+minSolutionComodo.toString());
			//-----------------------------------------------
			if(minSolutionComodo.getCosto()<minSolution.getCosto()){
				minSolution.setCostoTotale(minSolutionComodo.getCosto());
				minSolution.setListaInterventi(minSolutionComodo.getListaInterventi());
			}
			//PULISCO I VETTORI CHE TENGONO TRACCIA DEI MINIMI PER OGNI SOLUZIONE ESPLPOSA
			vetExecSquadre.clear();
			for(int z=0;z<n;z++)
				vetCostSquadre[z]=0;
		}
		
		openFile("FinalSolution");
		System.out.print(minSolution.toString());
		printToFile("\n"+minSolution.toString()+"\n");
		closefile();
	}

	
	
	/**
	 * Metodo di supporto per la creazione di una soluzione in formato esteso
	 * @param index numero della squadra
	 * @param squdre numero totale di squadre
	 * @return String rappresentate la colonna della matrice soluzione
	 */
	private static String getVector(int index, int squdre){
		String vector=new String();
		for(int i=0;i<squdre;i++){
			if(i!=index)
				vector+="0";
			else
				vector+="1";
		}
		return vector;
	}
	
	/**
	 * Metodo di supporto per importare da DB il costo dei carburanti
	 */
	public static void initParametriCosto(){
		//Setto il costo al listo dei vari carburanti
		dbOpenConnection(host, db, user, pass);
		try {
			myStatement = myConnection.createStatement();
			myResultSet = myStatement.executeQuery("SELECT * FROM datiglobali");
			myResultSet.next();
			costoBenzina = myResultSet.getFloat("costoBenzina");
			costoDiesel = myResultSet.getFloat("costoDiesel");
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		
	}
	
	/**
	 * Metodo di supporto per l'estrazione del tipo di alimentazione delle vetture delle squadre
	 * ed il consumo per 100km 
	 * @return ResultSet della query contentente una tabella con idSqaudra idCaratteristica alimentazione e consumo
	 */
	public static ResultSet initAutomezzoSquadre(){
		dbOpenConnection(host, db, user, pass);
		try{
			myStatement = myConnection.createStatement();
			
			return myResultSet = myStatement.executeQuery("" +
					"SELECT squadra.idSquadra, risorsaautomezzo.caratteristicheTecniche, " +
					"caratteristichetecniche.alimentazione, caratteristichetecniche.consumo, " +
					"caratteristichetecniche.costiProporzionali " +
					"from squadra join risorsaautomezzo on squadra.idAutomezzo = " +
					"risorsaautomezzo.idAutomezzo join caratteristichetecniche on " +
					"risorsaautomezzo.caratteristichetecniche = caratteristichetecniche.idCaratteristica;");
		}
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.getMessage());
			return myResultSet = null;
		}
	}
	
	/**
	 * Metodo di supporto per l'estrazione delle informazioni dei singoli interventi
	 * in particolare l'id dell'impinato in cui si trova l'intervento e la durata dell'intervento
	 * 
	 * @return ResultSet della query che genera una tabella con idIntervento, durata e idImpianto
	 */
	public static ResultSet getInfoInterventi(){
		dbOpenConnection(host, db, user, pass);
		try{
			myStatement = myConnection.createStatement();
			return myResultSet = myStatement.executeQuery("SELECT intervento.idIntervento, " +
					"intervento.durata, intervento.idImpianto " +
					"FROM intervento join impianto on intervento.idImpianto = impianto.idImpianto " +
					"order by idIntervento asc;");
		}
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.getMessage());
			return myResultSet = null;
		}
	}
	
	
	/**
	 * Metodo per la determinazione del costo minimo CONSIDERANDO l'ordine di esecuzione di una lista di interventi 
	 * affidati a una squadra
	 * 
	 * @param listaInterventi array di interi rappresentati gli id degli interventi
	 * @param idSquadra String rappresentante l'id della squadra
	 * @return costo di esecuzione
	 */
	public static float costoIntervetiPerSquadra(int[] listaInterventi,String idSquadra){
		float costoTotale;
		float minutiGiornataLavorativa=480;
		float minutiLavoroNormale;
		float minutiLavoroStraordinario;
		float costoOrarioNormale= 0;
		float costoOrarioStraordinario= 0;
		float ore;
		//double costoUsuraAutomezzo=0.0;
		
		float consumoAutomezzo=0;
		float distanzaTotale=0;
		float durataSpostamenti=0;
		float durataInterventi=0;
		float consumoCarburanteTotale=0;
		float durataGiornataTotale=0;
		String idImpiantoAttuale = idSedediPartenza;
		String idImpiantoSuccessivo;
		
		//Setting del consumo dell'automezzo
		
		consumoAutomezzo = squadre.get(Integer.valueOf(idSquadra)).getConsumo();
		
		//Per il calco del costo necessito di calcolare la lunghezza degli spostamenti e la durata
		
		//Calcolo la distanza totale per visitare tutti gli impianti della lista di interventi
		for(int i=0;i<listaInterventi.length;i++){
			//CONTROLLARE QUESTA ISTRUZIONE
			int indice = listaInterventi[i]-1; //Controllare
			
			idImpiantoSuccessivo = interventi.get(indice).getIdImpianto();
			
			if(! (idImpiantoAttuale.equals(idImpiantoSuccessivo)) ){
				//AGGIORNO LA DISTANZA TOTALE PERCORSA
				
				distanzaTotale+=getDistanzaSpostamentiImpinatiHash(idImpiantoAttuale, idImpiantoSuccessivo);
				//distanzaTotale+=getDistanzaImpianti(idImpiantoAttuale, idImpiantoSuccessivo);
				
				//AGGIORNO IL TEMPO TOTALE DEGLI SPOSTAMENTI
				//durataSpostamenti+=getDurataSpostamentiImpinati(idImpiantoAttuale, idImpiantoSuccessivo);
				durataSpostamenti+=getDurataSpostamentiImpinatiHash(idImpiantoAttuale, idImpiantoSuccessivo);
				
				//AGGIORNO IL TEMPO TOTALE DI INTERVENTI
				durataInterventi+=interventi.get(indice).getDurata();
				
				//IMPOSTO AGGIORNO L'IMPIANTO ATTUALE
				idImpiantoAttuale = idImpiantoSuccessivo;
			}
			else{
				durataInterventi+=interventi.get(indice).getDurata();
			}
			
		}
		
		//AGGIUNGO ALLA DISTANZA PERCORSA E AL TEMPO DI SPOSTAMENTO IL RITORNO ALLA SEDE DI PARTENZA
		//SE NON SONO GIA' NELLA SEDE DI PARTENZA
		if(! (idImpiantoAttuale.equals(idSedediPartenza)) ){
			
			distanzaTotale+=getDistanzaSpostamentiImpinatiHash(idImpiantoAttuale, idSedediPartenza);
			durataSpostamenti+=getDurataSpostamentiImpinatiHash(idImpiantoAttuale, idSedediPartenza);
			//distanzaTotale+=getDistanzaImpianti(idImpiantoAttuale, idSedediPartenza);
			//durataSpostamenti+=getDurataSpostamentiImpinati(idImpiantoAttuale, idSedediPartenza);
		}
		
		//RICAVO IL CONSUMO DI CARBURANTE & E I MINUTI PER ESEGUIRE INTERVENTI E SPOSTAMENTI
		consumoCarburanteTotale = (consumoAutomezzo * distanzaTotale / 100);
		durataGiornataTotale = durataSpostamenti + durataInterventi;
		
		
		//Ricavo costo in termini di euro
		
		//Determino se e' presente del lavoro da svolgere in tariffa di straordinario
		minutiLavoroStraordinario = durataGiornataTotale - minutiGiornataLavorativa;
		if(minutiLavoroStraordinario > 0){
			minutiLavoroNormale = minutiGiornataLavorativa;
		}
		else{
			minutiLavoroNormale = durataGiornataTotale;
		}
		
		//Calcolo il costo del lavoro della squadra in tariffa normale
		costoOrarioNormale =squadre.get(Integer.valueOf(idSquadra)).getCostoOrarioSquadra();
		ore = minutiLavoroNormale / 60;
		costoOrarioNormale *= ore;
		
		float costoConsumo = 0;
		
		//Controllo se la lista di interventi della squadra ha interventi o meno da fare
		// e  in caso positivo calcolo il costo consumo totale dell'automezzo
		
		//CALCOLO IL CONSUMO TOTALE TRA CARBURANTE E COSTO PROPORZIONALE
		//if(listaInterventi.length > 0 && consumoCarburanteTotale != 0){
			if(squadre.get(Integer.valueOf(idSquadra)).getAlimentazioneAutomezzo().equals("Benzina")){
				costoConsumo = (consumoCarburanteTotale * costoBenzina) + (squadre.get(Integer.valueOf(idSquadra)).getCostoProporzionale());
			}
			else{
				costoConsumo = (consumoCarburanteTotale) * costoDiesel + (squadre.get(Integer.valueOf(idSquadra)).getCostoProporzionale());
				
			}
		//}
		//else{
		//	costoConsumo = 0;
		//}
		
		//Calcolo il costo del lavoro della squadra in tariffa straordinaria
		if(minutiLavoroStraordinario>0){
			//Fattore di moltiplicazione per il costo del lavoro straordinario
			costoOrarioStraordinario = (squadre.get(Integer.valueOf(idSquadra)).getCostoOrarioSquadra())*2;
			ore = minutiLavoroStraordinario / 60;
			costoOrarioStraordinario *= ore;
			
			
		}
		//Calcolo il costo totale monetario della squadra
		costoTotale = costoOrarioNormale + costoOrarioStraordinario + costoConsumo;
		
		return costoTotale;
		
	}
	
	
	
	/**
	 * Metdo di supporto per determinare se una data squadra ha tutti le competenze per eseguire una lista di interveni
	 * @param listaInterventi String contenente una lista di interventi (gli id degli interventi)
	 * @param idSquadra id della squadra che deve eseguire gli nterventi
	 * @return boolena True se la suqadra possiede tutte le compentenze per la lista di interventi, False antrimenti
	 */
	public static boolean checkListaInterventi(String listaInterventi,String idSquadra){
		
		for(int i=0;i<listaInterventi.length();i++){
			if(!checkIntSquad(Integer.valueOf((String)listaInterventi.subSequence(i, i+1)), Integer.valueOf(idSquadra)))
			return false;
		}
		return true;
		
	}
	
	/**
	 * Metodo di supporto per verificare se un dato intervento può essere eseguito da una specifica squadra
	 * @param indiceIntervento 
	 * @param indiceSquadra
	 * @return true se l'intervetnto puo' essere eseguito dalla squadro, false altrimenti
	 */
	public static boolean checkIntSquad(int indiceIntervento, int indiceSquadra){
		
		
		ArrayList<String> compIntervneto = interventi.get(indiceIntervento).getCompetenze();
		ArrayList<String> compSquadra = squadre.get(indiceSquadra).getCompentenze();
		
		for(int i=0; i < compIntervneto.size(); i++){
			if(!compSquadra.contains(compIntervneto.get(i))){
				return false;
			}	
		}
		return true;
	}
	
	/**
	 * Metodo di supporto per il calcolo del costo monetario. Il metodo estrae dal DB i valori
	 * riguardanti il costo orario delle risorse componeti una squadra
	 * @param idSquadra id della squadra
	 * @return double con il costo orario totale della squadra
	 */
	public static float getCostoOrarioSquadra(String idSquadra){
		float costoOra=0;
		dbOpenConnection(host, db, user, pass);
		try{
			myStatement = myConnection.createStatement();
			myResultSet = myStatement.executeQuery("Select costoOra, idOperatore " +
					"From risorsaoperativa where idSquadra="+idSquadra+";");
			while(myResultSet.next()){
				costoOra += myResultSet.getFloat("costoOra");
			}
			return costoOra;
		}
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.getMessage());
			return -1;
		}
		finally{
			dbCloseConnection();
		}
	}
	
	/**
	 * Metodo di supporto per il setting dei costi del carburante
	 */
	public static void setCostoCarburanti(){
		dbOpenConnection(host, db, user, pass);
		try{
			myStatement = myConnection.createStatement();
			myResultSet = myStatement.executeQuery("Select * From datiglobali;");
			
			myResultSet.next();
			
			costoBenzina = myResultSet.getFloat("costoBenzina");
			costoDiesel = myResultSet.getFloat("costoDiesel");
		}
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		finally{
			dbCloseConnection();
		}
	}
	
	/**
	 * Metodo di supporto per ricavare il tempo di spostamento tra due impianti
	 * @param idOrigine id dell'impianto di partenza
	 * @param idDestinazione id dell'impinato di destinazione
	 * @return double della tempo di percorrenza
	 */
	/*
	public static double getDurataSpostamentiImpinati(String idOrigine, String idDestinazione){
		double durata;
		dbOpenConnection(host, db, user, pass);
		try{
			myStatement = myConnection.createStatement();
			myResultSet = myStatement.executeQuery("Select durata From distanzeimpianti " +
			"where (idImpianto1="+idOrigine+" and idImpianto2="+idDestinazione+") " +
			"or (idImpianto2="+idOrigine+" and idImpianto1="+idDestinazione+");");
			
			myResultSet.next();
			
			durata = myResultSet.getDouble("durata");
			return durata;
		}
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.getMessage());
			durata=-1.0;
			return durata;
		}
		finally{
			dbCloseConnection();
		}
	}
	*/
	
	public static float getDurataSpostamentiImpinatiHash(String idOrigine, String idDestinazione){
		
		float durata=0;
		
		for(int i=0;i<distanzeImpianti.size();i++){
			
			if((distanzeImpianti.get(i).get("idImpianto1").equals(idOrigine)) && (distanzeImpianti.get(i).get("idImpianto2").equals(idDestinazione))){
				durata= Float.valueOf(distanzeImpianti.get(i).get("durata"));
				return durata;
			}
			if((distanzeImpianti.get(i).get("idImpianto2").equals(idOrigine)) && (distanzeImpianti.get(i).get("idImpianto1").equals(idDestinazione))){
				durata= Float.valueOf(distanzeImpianti.get(i).get("durata"));
				return durata;
			}
		}
		
		return durata;
	}
	
	/**
	 * Metdo di supporto per la creazione di una mappa contenete i valodi di distanze e tempi 
	 */
	public static void initMapSpostamenti(){
		
		dbOpenConnection(host, db, user, pass);
		try{
			myStatement = myConnection.createStatement();
			myResultSet = myStatement.executeQuery("Select * From distanzeimpianti; ");
			ResultSetMetaData myMeta = myResultSet.getMetaData();
			
			while(myResultSet.next()){
				HashMap<String, String> row = new HashMap<String, String>();
				row.put(myMeta.getColumnName(1), myResultSet.getString(1));
				row.put(myMeta.getColumnName(2), myResultSet.getString(2));
				row.put(myMeta.getColumnName(3), myResultSet.getString(3));
				row.put(myMeta.getColumnName(4), myResultSet.getString(4));
				
				distanzeImpianti.add(row);
			}
		}
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.getMessage());
			
		}
		finally{
			dbCloseConnection();
		}
	}
	/**
	 * Metodo di supporto per ricavare la distanza tra due impiantia
	 * @param idOrigine id dell'impianto di partenza
	 * @param idDestinazione id dell'impinato di destinazione
	 * @return double della distanza tra i due impinati
	 */
	
	/*
	public static double getDistanzaImpianti(String idOrigine, String idDestinazione){
		double distanza;
		dbOpenConnection(host, db, user, pass);
		try{
			myStatement = myConnection.createStatement();
			myResultSet = myStatement.executeQuery("Select distanza From distanzeimpianti " +
			"where (idImpianto1="+idOrigine+" and idImpianto2="+idDestinazione+") " +
			"or (idImpianto2="+idOrigine+" and idImpianto1="+idDestinazione+");");
			
			myResultSet.next();
			
			distanza = myResultSet.getDouble("distanza");
			return distanza;
		}
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.getMessage());
			distanza=-1.0;
			return distanza;
		}
		finally{
			dbCloseConnection();
		}
	}
	*/
	
	public static float getDistanzaSpostamentiImpinatiHash(String idOrigine, String idDestinazione){
		
		float distanza=0;
		
		for(int i=0;i<distanzeImpianti.size();i++){
			
			if((distanzeImpianti.get(i).get("idImpianto1").equals(idOrigine)) && (distanzeImpianti.get(i).get("idImpianto2").equals(idDestinazione))){
				distanza= Float.valueOf(distanzeImpianti.get(i).get("distanza"));
				return distanza;
			}
			if((distanzeImpianti.get(i).get("idImpianto2").equals(idOrigine)) && (distanzeImpianti.get(i).get("idImpianto1").equals(idDestinazione))){
				distanza= Float.valueOf(distanzeImpianti.get(i).get("distanza"));
				return distanza;
			}
		}
		
		return distanza;
	}
	
	/**
	 * Metodo per la generezazione delle permutazioni di un vettore di interi
	 * @param array vettore di interi con permutazione iniziale
	 * @return true se sono disponibili altre permutazioni false se non vi sono altre permutazioni possibili
	 */
	public static boolean onePermutation(int[]array){
		int len = array.length;
		int i = len - 1;
		while (i>0 && (array[i-1]) >= array[i] )
			i--;
		if(i==0)
			return false;
		int j= len;
		while(array[j-1] <= array[i-1])
			j--;
		swap(i-1,j-1,array);
		i++;
		j = len;
		while(i<j){
			swap(i-1, j-1,array);
			i++;
			j--;
		}
		return true;
	}
	
	/**
	 * Metdo di supporto per la ricerca di tutti i possibili ordini di esecuzione di una lista di interventi
	 * @param array Stringa contenente il primo ordine di esecuzione degli interventi
	 * @param idSquadra id della Squadra necessario per il calcolo del costo
	 * @return
	 */
	
	public static ArrayList<String> findMinCostOneNonOrder(ArrayList<Integer> listaInterventiSTART,String idSquadra ){
		
		
		
		ArrayList<String> listaCosto = new ArrayList<String>();
		
		int arrayComodo[]= new int[listaInterventiSTART.size()];
		int listaIntMin[] = new int[listaInterventiSTART.size()];
		float actualEsamsCost;
		float minCost;
		
		//Traformazione della stringa contenente il primo ordine di esecuzione
		//in un vettore di interi necessario per il metodo che genera le successive permutazioni
		
		//for(int i=0;i<listaInterventiSTART.length();i++){
		for(int i=0;i<listaInterventiSTART.size();i++){
			//arrayComodo[i] = Integer.valueOf(listaInterventiSTART.substring(i, i+1));
			arrayComodo[i]=listaInterventiSTART.get(i);
		}
		
		//Controllo se già è presente un file che contiene il valore minimo per quella lista di interventi e squadra
		File file = new File ("analisi"+stampa(arrayComodo)+"squadra::"+idSquadra+".txt");
		if(file.exists()){//Se esiste estrapolo il valore minimo
			
			try {
				FileReader myFileReader = new FileReader("analisi"+stampa(arrayComodo)+"squadra::"+idSquadra+".txt");
				BufferedReader myBufferReader = new BufferedReader(myFileReader);
				String line = myBufferReader.readLine();
				String linePenultima = new String();;
				while(line != null){
					linePenultima=line;
					line = myBufferReader.readLine();
					//StringTokenizer myToken = new StringTokenizer(line,":::");
					//while(myToken.hasMoreTokens()){
					//	System.out.println(myToken.nextToken());
					//}
				}
				StringTokenizer myTokenizer = new StringTokenizer(linePenultima,"::");
				while(myTokenizer.hasMoreTokens()){
					if(myTokenizer.nextToken().equals("listaInt")){
						listaCosto.add(myTokenizer.nextToken());
					}
					if(myTokenizer.nextToken().equals("costo")){
						listaCosto.add(myTokenizer.nextToken());
					}
				}
				myBufferReader.close();
				myFileReader.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return listaCosto;
		}
		else{//Se non esiste calcolo e creo il fiel
			
			//Calcolo del costo per l'esecuzione degli interventi nell'ordine della prima permutazione
			copyIntArray(arrayComodo, listaIntMin);
			
			System.out.println(stampa(arrayComodo));
			
			
			
			minCost = costoIntervetiPerSquadra(arrayComodo, idSquadra);
			
			//DEBUGGGGG
			openFile("analisi"+stampa(arrayComodo)+"squadra::"+idSquadra);
			
			printToFile("valuto squadra id:"+idSquadra+":listaInt:"+stampa(arrayComodo)+":costo:"+minCost+"\n");
			//int numero =1;
			/*
			String comodo = new String();
			for(int i=0; i<arrayComodo.length;i++)				
				comodo+=arrayComodo[i];
			//permutazioni.add(comodo);
			*/
			//Generazione di tutte le possibili permutazioni dell'ordine degli interventi e calcolo del costo migliore
			while(onePermutation(arrayComodo)){
				//numero++;
				/*comodo="";
				for(int i=0; i<arrayComodo.length;i++)				
					comodo+=arrayComodo[i];
				System.out.println(comodo);
				permutazioni.add(comodo);*/
				
				
				
				actualEsamsCost=costoIntervetiPerSquadra(arrayComodo, idSquadra);
				//DEBUGGGGG
				printToFile("valuto squadra id:"+idSquadra+":listaInt:"+stampa(arrayComodo)+":costo:"+actualEsamsCost+"\n");
				
				//Utility.stampa(arrayComodo);
				
				//System.out.println("costo attuale::"+actualEsamsCost);
				//System.out.println("costo minimo::"+minCost);
				//System.out.println("costo numero da permutare::"+arrayComodo.length+"!");
				//System.out.println("permutazioni fatte::"+numero);
				if(actualEsamsCost<minCost){
					copyIntArray(arrayComodo, listaIntMin);
					minCost = actualEsamsCost;
					//printToFile("soluzione migliore Provvisioria:"+stampa(listaIntMin)+"costo:"+minCost+"\n");
				}
			}
			
			//Creo la sringa rappresentante l'ordine di esecuzione degli interventi minimo
			String listaInterventiMIN = new String();
			for(int i=0;i<listaIntMin.length;i++){
				listaInterventiMIN+=listaIntMin[i];
			}
			listaCosto.add(listaInterventiMIN);
			listaCosto.add(String.valueOf(minCost));
			
			//DEBUGGGGG
			printToFile("soluzione migliore per squadra id:"+idSquadra+":listaInt:"+listaCosto.get(0)+":costo:"+listaCosto.get(1));
			closefile();
			
			//ritorno il valore di costo per l'esecuzione degli interventi considerato con costo minimo
			return listaCosto;
					
		}
	}
	
	/**
	 * Metodo di supporto che fa lo swap di due elementi in un vettore di interi
	 * @param i
	 * @param j
	 * @param array
	 */
	public static void swap(int i,int j,int[] array){
		int comodo = array[i];
		array[i]=array[j];
		array[j]=comodo;
	}
	
	/**
	 * Metodo di supporto che effettua la copia dei valori di un array in un'altro
	 * @param origine
	 * @param destinazione
	 */
	public static void copyIntArray(int[] origine, int[] destinazione){
		if(origine.length==destinazione.length){
			for(int i=0; i<origine.length; i++){
				destinazione[i] = origine[i];
			}
		}
		else{
			System.out.println("Errore nell'utilizzo del metodo Utility.copyIntArray");
		}
	}
	
	/**
	 * Metodo di supporto per la chiusura di un file aperto in precendenza
	 */
	static public void closefile() {
        //SimpleDateFormat dataFormat = new SimpleDateFormat("dd-MM-yyy-HH-mm-ss");
        //Date dateNow = new Date();
        //printToFile("End Simutated Annealing process: " + dataFormat.format(dateNow));

        try {
            myBufferWriter.flush();
            myBufferWriter.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

	/**
	 * Metodo di supporto per la scrittura all'interno di un file precedentemente aperto
	 * @param value valore da scrivere all'interno del file
	 */
    static public void printToFile(String value) {
        try {
            myBufferWriter.write(value);
            myBufferWriter.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

 
    /**
     * Metodo di supporto per l'apertura o creazione (nel caso il file non esista gia') di un file
     * @param nome String contenente il nome del file
     */
    static public void openFile(String nome) {
        // TODO Auto-generated method stub
        try {
            //SimpleDateFormat dataFormat = new SimpleDateFormat("dd-MM-yyy-HH-mm-ss");
            //Date dateNow = new Date();
            //myWriter = new FileWriter(nome+ " "+ dataFormat.format(dateNow) + ".txt");
            myWriter = new FileWriter(nome+".txt",true);
            myBufferWriter = new BufferedWriter(myWriter);
            //printToFile("Start Simutated Annealing process: " + dataFormat.format(dateNow));

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * Metodo di supporto per la stampa di un array di interi
     * @param array
     * @return String 
     */
    public static String stampa(int [] array){
    	String result = new String();
		for(int i=0;i<array.length;i++){
			result+=""+array[i];
		}
		return result;
	}
}