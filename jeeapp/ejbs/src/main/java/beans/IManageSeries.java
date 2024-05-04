package beans;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Remote;

import book.Invite;
import book.UserActor;
import jpaprimer.generated.Serie;

@Remote
public interface IManageSeries {
	/**
	 * Gets All the Series in the Database and returns a list of Series
	 * 
	 * @return
	 */
	public List<Serie> getSeries();

	/**
	 * Returns a list with all the Series Titles in the database
	 */
	public List<String> getSeriesTitles();

	/**
	 * Returns a List of all the Actors and Stars names in all the Series in the DB
	 */
	public List<String> getAllActors();

	public List<String> getAbove(BigDecimal score, BigInteger NumberOfVotes);

	public List<String> genresMatch(String genre);

	public Map<String, Integer> genresActors(String genre);

	public List<String> atLeastOneKeyword(List<String> keyword);

	public List<String> allKeywords(List<String> keyword);

	public Map<String, BigDecimal> genresRating();

	public Set<Serie> genreSeries(String genre);

	public List<String> directorsList();

	public void addSerie(Serie serie);

	public boolean addUserActor(UserActor user);

	public boolean login(UserActor user);

	public boolean addInvite(Invite i);

	public List<Invite> getInvitationsList();

	public List<String> getInvitationReplys(String title);

	public void acceptInvitation(String invite, String actor);

	public String addActorToSerie(String title, String choosenActor);

	public List<Serie> getDirectoSeries(String director);
}