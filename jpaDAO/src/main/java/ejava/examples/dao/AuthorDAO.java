package ejava.examples.dao;

import ejava.examples.dao.domain.Author;

public interface AuthorDAO {

    public abstract void create(Author author) throws DAOException;

    public abstract Author get(long id) throws DAOException;

    public abstract Author getByQuery(long id) throws DAOException;

    public abstract Author update(Author author) throws DAOException;

    public abstract Author updateByMerge(Author author) throws DAOException;

    public abstract void remove(Author author) throws DAOException;

}