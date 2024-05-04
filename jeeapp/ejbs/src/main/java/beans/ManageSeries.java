package beans;

import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import book.Invite;
import book.UserActor;
import jpaprimer.generated.Serie;

@Stateless
public class ManageSeries implements IManageSeries {
	@PersistenceContext(unitName = "playSeriesShow")
	EntityManager em;

	/**
	 * Gets All the Series in the Database and returns a list of Series
	 * 
	 * @return
	 */
	public List<Serie> getSeries() {
		System.out.println("Getting Series from DB...");
		TypedQuery<Serie> q = em.createQuery("FROM Serie s", Serie.class);
		List<Serie> list = q.getResultList();
		return list;
	}

	/**
	 * Returns a list with all the Series Titles in the database
	 */
	public List<String> getSeriesTitles() {
		System.out.println("Getting Series from DB...");
		TypedQuery<Serie> q = em.createQuery("from Serie s", Serie.class);
		List<Serie> series = q.getResultList();
		List<String> ss = new ArrayList<>();

		for (Serie s : series) {
			ss.add(s.getTitle());
		}
		return ss;
	}

	/**
	 * Returns a List of all the Actors and Stars names in all the Series in the DB
	 */
	public List<String> getAllActors() {
		System.out.println("Retrieving the actors from the database...");
		TypedQuery<Serie> q = em.createQuery("FROM Serie s", Serie.class);
		List<Serie> list = q.getResultList();
		List<String> actors = new ArrayList<>();
		for (Serie s : list) {
			for (String act : s.getActor()) {
				if (!actors.contains(act)) {
					actors.add(act);
				}
			}
			for (String act : s.getStar()) {
				if (!actors.contains(act)) {
					actors.add(act);
				}
			}
		}
		return actors;
	}

	/**
	 * Return a list of the series which the score and the number of votes are above
	 * the given parameters
	 * 
	 * @param score         allowed object is {@link BigDecimal }
	 * @param NumberOfVotes object is {@link BigInteger }
	 * 
	 * @return List of series Titles above the parameters
	 */
	public List<String> getAbove(BigDecimal score, BigInteger NumberOfVotes) {
		List<Serie> series = getSeries();
		List<String> serieAbove = new ArrayList<>();
		System.out.println("Comparig the values...");
		for (Serie s : series) {
			BigInteger votes = s.getNumberOfVotes();
			BigDecimal rating = s.getScore();
			if (votes.compareTo(NumberOfVotes) == 1 && rating.compareTo(score) == 1) {
				serieAbove.add(s.getTitle());
			}
		}
		return serieAbove;
	}

	/**
	 * Given a Genre, obtains all Series that match that genre
	 * 
	 * @param genre object is a {@link String}
	 */
	public List<String> genresMatch(String genre) {
		List<Serie> series = getSeries();
		List<String> list = new ArrayList<>();
		for (Serie serie : series) {
			if (serie.getGenre().contains(genre)) {
				list.add(serie.getTitle()); // Estou a adicionar o título da Série de qual o Género faz parte mas ainda
											// nao sei se é para adicionar a serie toda ou basta o titulo
			}
		}
		return list;
	}

	/**
	 * Given a Genre, obtains all Series that match that genre
	 * 
	 * @param genre object is a {@link String}
	 */
	public Map<String, Integer> genresActors(String genre) {
		List<Serie> series = getSeries();
		Map<String, Integer> actorsNumbers = new HashMap<String, Integer>();

		for (Serie serie : series) { // Os atores têm de ser ordenados
			if (serie.getGenre().contains(genre)) {
				for (String actor : serie.getStar()) {
					System.out.println("star" + actor);
					if (actorsNumbers.containsKey(actor)) {
						Integer numberOfTimes = actorsNumbers.get(actor);
						actorsNumbers.replace(actor, numberOfTimes, numberOfTimes + 1);
					} else {
						actorsNumbers.put(actor, 1);
					}
				}
				for (String actor : serie.getActor()) {
					System.out.println("actor" + actor);
					if (actorsNumbers.containsKey(actor)) {
						Integer numberOfTimes = actorsNumbers.get(actor);
						actorsNumbers.replace(actor, numberOfTimes, numberOfTimes + 1);
					} else {
						actorsNumbers.put(actor, 1);
					}
				}
			}
		}
		Map<String, Integer> sorted = actorsNumbers.entrySet().stream()
				.sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
		return sorted;
	}

	/**
	 * Given a set of Keywords, obtains the Series titles that inclued at least of
	 * of the keywords in the Description of the Serie
	 */
	public List<String> atLeastOneKeyword(List<String> keyword) {
		List<Serie> series = getSeries();
		List<String> list = new ArrayList<>();
		for (String key : keyword) {
			for (Serie serie : series) {
				if (serie.getSummaryOfSerie().toLowerCase().contains(key)) {
					if (!list.contains(serie.getTitle())) {
						list.add(serie.getTitle());
					}
				}
			}
		}
		return list;
	}

	/**
	 * Given a set of Keywords, obtains the Series titles that inclued all the
	 * keywords in the description of the Serie
	 */
	public List<String> allKeywords(List<String> keyword) {
		List<Serie> series = getSeries();
		List<String> list = new ArrayList<>();

		for (Serie serie : series) {
			boolean b = true;
			for (String key : keyword) {
				if (!serie.getSummaryOfSerie().toLowerCase().contains(key)) {
					b = false;
				}
			}
			if (b == true) {
				list.add(serie.getTitle());
			}
		}
		return list;
	}

	public Map<String, BigDecimal> genresRating() {
		@SuppressWarnings("unchecked")
		List<Object[]> results = (List<Object[]>) em
				.createNativeQuery(
						"select distinct genre, round(avg(s.score),2) as n " + "from serie as s, serie_genre as sg "
								+ "where sg.serie_id = s.id " + "group by sg.genre " + "FETCH NEXT 30 ROWS ONLY")
				.getResultList();
		Map<String, BigDecimal> avgRatingByGenre = new HashMap<>();
		int i = 1;
		for (Object[] o : results) {
			String g = (String) o[0];
			BigDecimal avg = (BigDecimal) o[i];
			System.out.println(g + "_" + avg);
			avgRatingByGenre.put(g, avg);

		}

		return avgRatingByGenre;
	}

	public Set<Serie> genreSeries(String genre) {
		System.out.println(capitalise(genre));
		System.out.println("Getting Things Done");
		TypedQuery<Serie> q = em.createQuery("FROM Serie s", Serie.class);
		Set<Serie> list = (Set<Serie>) q.getResultList().stream().collect(Collectors.toSet());
		Set<Serie> series = new HashSet<>();
		for (Serie s : list) {
			System.out.println("From List ---- " + s.getTitle());
			System.out.println(s.getGenre());
			if (s.getGenre().contains(capitalise(genre))) {
				series.add(s);
				System.out.println("Adicionou - " + s.getTitle());
			}
		}
		return series;
	}

	public List<String> directorsList() {
		System.out.println("Getting the Directors....");
		@SuppressWarnings("unchecked")
		List<String> directors = (List<String>) em
				.createNativeQuery("select distinct director from serie_director order by director asc")
				.getResultList();
		System.out.println(directors.toString());
		return directors;
	}

	public void addSerie(Serie s) {
		System.out.println("Adding the Serie to the DB...");
		em.persist(s);
		System.out.println("Serie Added to the DB!");
	}

	// ActorsNode Methods

	@SuppressWarnings("unchecked")
	private List<String> getUsernames() {
		List<String> users = em.createNativeQuery("select u.username from useractor as u").getResultList();
		return users;
	}

	public boolean addUserActor(UserActor user) {
		boolean added = false;
		System.out.println("Verifying if username already exists");
		List<String> users = getUsernames();
		if (!users.contains(user.getUsername())) {
			System.out.println("Adding the user to DB!");
			em.persist(user);
			added = true;
			System.out.println(user.getUsername() + " added to DB.");
		}
		return added;
	}

	public boolean login(UserActor user) {
		boolean loginOk = false;
		String username = user.getUsername();
		String password = user.getPassword();
		System.out.println(username + " " + password);
		System.out.println("[ManageSeries] getting login ready");
		try {
			@SuppressWarnings("unchecked")
			List<UserActor> userOk = em.createNativeQuery(
					"select username, password from useractor where username = :username and password = :password")
					.setParameter("username", username).setParameter("password", password).getResultList();
			System.out.println("Query Enviada");
			if (userOk.size() == 1) {
				loginOk = true;
			} else {
				System.out.println("[ManageSerie] Não funcionou o login.......");
			}
		} catch (Exception e) {
			System.out.println("Erro ManageSeries: " + e);
		}

		return loginOk;
	}

	public boolean addInvite(Invite i) {
		boolean added = false;
		System.out.println("Verifying if invite already exists");
//		List<Invite> invites = getInvitationsList();
		List<String> invitesTitles = em.createNativeQuery("select title from invite").getResultList();
		System.out.println(invitesTitles);
		if (!invitesTitles.contains(i.getTitle())) {
			System.out.println("Adding invitation to DB!");
			em.persist(i);
			added = true;
			System.out.println(i.getTitle() + " Invitation added to DB.");
		}
		return added;
	}

	public List<Invite> getInvitationsList() {
		System.out.println("Getting Invitations from DB...");
		List<Invite> invitations = em.createQuery("FROM Invite i", Invite.class).getResultList();
		return invitations;
	}

	public List<String> getInvitationReplys(String title) {
		System.out.println("Getting the Replys to Invitation " + title);
		@SuppressWarnings("unchecked")
		List<String> replys = em
				.createNativeQuery("Select i.reply FROM invite_reply as i, invite as ii where ii.title=:title")
				.setParameter("title", title).getResultList();
		System.out.println(replys);
		return replys;
	}

	public void acceptInvitation(String title, String actor) {
		List<Invite> invitations = getInvitationsList();
		for (Invite x : invitations) {
			if (x.getTitle().equals(title)) {
				System.out.println("Got the Invite to Accept");
				List<String> actors = x.getReply();
				System.out.println("[Manage] Got the actors List");
				if (!actors.contains(actor)) {
					actors.add(actor);
				} else
					System.out.println("Already in the list!");
				System.out.println(actors);
				x.setReply(actors);
				em.refresh(x);
				System.out.println("Invitation Merder with new Actor");
			}
		}
	}

	public String addActorToSerie(String title, String choosenActor) {
		List<Serie> series = getSeries();
		String confirmation = " ";
		for (Serie s : series) {
			if (s.getTitle().equals(title)) {
				System.out.println("[ManageSeries] " + s.getTitle());
				Set<String> actors = s.getActor();
				System.out.println(actors);
				actors.add(choosenActor);
				s.setActor(actors);
				System.out.println(actors);
				em.refresh(s);
				confirmation = "Confirmed";
			}
		}
		return confirmation;
	}

	public List<Serie> getDirectoSeries(String director) {
		List<Serie> allSeries = getSeries();
		List<Serie> directorSeries = new ArrayList<>();
		for (Serie s : allSeries) {
			if (s.getDirector().contains(director)) {
				directorSeries.add(s);
				System.out.println(s.getTitle() + " Added to the Directors Series List");
			}
		}
		return directorSeries;
	}

	// Auxiliares
	private static String capitalise(String str) {
		return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
	}
}
