package ejava.examples.ejbwar.inventory.rs;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ejava.examples.ejbwar.inventory.bo.Category;
import ejava.examples.ejbwar.inventory.ejb.InventoryMgmtEJB;

@Path("categories")
public class CategoriesResource {
	private static final Log log = LogFactory.getLog(CategoriesResource.class);
	
	@Inject
	private InventoryMgmtEJB ejb;
	@Context
	private Request request;
	@Context 
	private UriInfo uriInfo;
	
	@GET @Path("")
	@Produces(MediaType.APPLICATION_XML)
	public Response getCategories(
			@QueryParam("offset")@DefaultValue("0") int offset,
			@QueryParam("limit")@DefaultValue("0") int limit) {
		log.debug(String.format("%s %s", request.getMethod(), uriInfo.getAbsolutePath()));

		try {
			List<Category> categories = ejb.getCategories(offset, limit);
				//wrap java.lang.List in object that preserves type for marshalling
			return Response.ok(new GenericEntity<List<Category>>(categories){})
					.build();
		} catch (Exception ex) {
			String message = String.format("unexpected error getting categories:%s", 
					ex.getLocalizedMessage());
			log.warn(message, ex);
			return Response.serverError()
					.entity(message)
					.type(MediaType.TEXT_PLAIN)
					.build();
		}
	}
	
	@GET @Path("{id}")
	@Produces(MediaType.APPLICATION_XML)
	public Response getCategory(@PathParam("id")int id) {
		log.debug(String.format("%s %s", request.getMethod(), uriInfo.getAbsolutePath()));

		try {
			Category category = ejb.getCategory(id);
			if (category != null) {
				return Response.ok(category)
						.tag("" + category.getVersion())
						.build();
			}
			else {
				return Response.status(Response.Status.NOT_FOUND)
						.entity(String.format("unable to locate category %d", id))
						.type(MediaType.TEXT_PLAIN)
						.build();
			}
		} catch (Exception ex) {
			String message = String.format("unexpected error getting category:%s", 
					ex.getLocalizedMessage());
			log.warn(message, ex);
			return Response.serverError()
					.entity(message)
					.type(MediaType.TEXT_PLAIN)
					.build();
		}
	}
}
