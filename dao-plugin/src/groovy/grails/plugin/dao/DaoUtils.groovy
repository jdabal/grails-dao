package grails.plugin.dao

import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.springframework.transaction.interceptor.TransactionAspectSupport
import grails.validation.ValidationException
import org.codehaus.groovy.grails.commons.ApplicationHolder as AH
import org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.dao.DataAccessException
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.support.RequestContextUtils
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.beans.BeansException

import java.util.Locale

/**
* A bunch of statics to support the GormDaoSupport.
* this is also setup as daoUtilsBean so that it gets injected with the ApplicationContext once its setup
*/
class DaoUtils implements ApplicationContextAware 
{
	
	static ApplicationContext ctx
	//static def sessionFactory
	//static def messageSource
	
	//void setSessionFactory(s){ sessionFactory=s }
	//void setMessageSource(m){ messageSource=m }

	public void setApplicationContext(ApplicationContext ctx) throws BeansException { 
		this.ctx = ctx
	}
	
	/**
	* checks the passed in version with the version on the entity (entity.version)
	* make sure entity.version is not greater
	*
	* @param  entity  the domain object the check
	* @param  ver the version this used to be (entity will have the )
	* @throws GormDataException adds a rejectvalue to the errors on the entity and throws with code optimistic.locking.failure
	*/
	static void  checkVersion(entity,ver){
		if (ver == null) return
		def version = ver.toLong()
		if (entity.version > version) {
			def msgMap = optimisticLockingFailureMessage(entity)
			entity.errors.rejectValue("version", msgMap.code, msgMap.args as Object[],msgMap.defaultMessage)
			throw new GormDataException(msgMap, entity, entity.errors)
		}
	}

	/**
	* check that the passed in entity is not null and throws GormDataException setup with the notfound message
	*
	* @param  entity  the domain object the check
	* @param  params  the params map
	* @param  domainClassName  the name of the domain
	* @throws GormDataException if it not found
	*/
	static void checkFound(entity, Map params,String domainClassName){
		if (!entity) {
			throw new GormDataException(notFoundResult(domainClassName,params), null)
		}
	}

	/**
	* returns a messageMap setup for a not found error
	*
	* @param  id  the id of the object that was not found
	*/
	static Map notFoundResult(domainClassName,params) {
		def domainLabel = GrailsClassUtils.getShortName(domainClassName)
		return [code:"default.not.found.message",args:[domainLabel,params.id],
			defaultMessage:"${domainLabel} not found with id ${params.id}"]
	}

	/**
	* just a short method to return a messageMap from the vals passed in
	*/
	static def setupMessage(messageCode,args,defaultMessage=""){
		return [code:messageCode,args:args,defaultMessage:defaultMessage]
	}
	
	static Map buildMessageParams(entity){
		def ident = badge(entity.id,entity)
		def domainLabel = resolveDomainLabel(entity)
		def args = [domainLabel, ident]
		return [ident:ident,domainLabel:domainLabel,args:args]
	}

	static Map createMessage(entity){
		def p = buildMessageParams(entity)
		return setupMessage("default.created.message",p.args,"${p.domainLabel} ${p.ident} created")
	}
	
	static Map saveMessage(entity){
		def p = buildMessageParams(entity)
		return setupMessage("default.saved.message",p.args,"${p.domainLabel} ${p.ident} saved")
	}
	
	static Map saveFailedMessage(entity){
		def domainLabel = resolveDomainLabel(entity)
		return setupMessage("default.not.saved.message",[domainLabel],"${domainLabel} save failed")
	}
	
	static Map updateMessage(entity,success=true){
		def p = buildMessageParams(entity)
		if(!success){
			return setupMessage("default.not.updated.message",p.args, "${p.domainLabel} ${p.ident} update failed")
		} else {
			return setupMessage("default.updated.message",p.args,"${p.domainLabel} ${p.ident} updated")
		}
	}
	
	static Map deleteMessage(entity,ident,success=true){
		def domainLabel = resolveDomainLabel(entity)
		if(!success){
			return setupMessage("default.not.deleted.message",[domainLabel,ident],"${domainLabel} ${ident} could not be deleted")
		}
		return setupMessage("default.deleted.message",[domainLabel,ident],"${domainLabel} ${ident} deleted")
	}
	static Map optimisticLockingFailureMessage(entity){
		def domainLabel = resolveDomainLabel(entity)
		def msgMap = setupMessage("default.optimistic.locking.failure",[domainLabel],"Another user has updated the ${domainLabel} while you were editing")
	}
	
	static String resolveDomainLabel(entity){
		return resolveMessage("${propName(entity.class.name)}.label", "${GrailsClassUtils.getShortName(entity.class.name)}")
	}
	
	static String resolveMessage(code,defaultMsg){
		//def ctx2 = AH.application.mainContext
		//println "default locale is "+ defaultLocale()
		//println "code  is "+ code
		def msg = ctx.messageSource.getMessage(code, [] as Object[] ,defaultMsg,  defaultLocale())
		return msg
	}
	
	static Locale defaultLocale(){
		try {
			GrailsWebRequest webRequest = RequestContextHolder.currentRequestAttributes()
			Locale currentLocale = RequestContextUtils.getLocale(webRequest.getCurrentRequest())
			return currentLocale
		}
		catch (java.lang.IllegalStateException e) {
		    return Locale.ENGLISH
		}
	}

	static String propName(String prop){
		def cname = GrailsClassUtils.getShortName(prop)
		def firstCharString = cname.charAt(0).toLowerCase().toString()
		cname = firstCharString + cname.substring(1)
		return GrailsClassUtils.getPropertyForGetter(cname)?:cname
	}

	//used for messages, if the entity has a name field then use that other wise fall back on the id and return that
	static def badge(id,entity){
		def hasName = entity?.metaClass.hasProperty(entity,'name')
		return ((hasName && entity) ? entity.name : id)
	}

	//forces a rollback on the existing transaction
	static void rollback(){
		TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
	}

	/*this is basically copied from grails ClosureEventTriggeringInterceptor
	* it fires the passed in event, first trying a method and then a closure if it exists
	*/
	static boolean triggerEvent(dao, String event, entity, params) {
		def result = false
		def eventTriggered = false
		if(dao.respondsTo(event,Object) || dao.respondsTo(event,Object,Object) ) {
			eventTriggered = true
			if(params){
				result = dao."$event"(entity,params)
			}else{
				result = dao."$event"(entity)
			}
			if(result instanceof Boolean) result = !result
			else {
				result = false
			}
		}
		else if(dao.hasProperty(event)) {
			eventTriggered = true
			def callable = dao."$event"
			if(callable instanceof Closure) {
				callable.resolveStrategy = Closure.DELEGATE_FIRST
				callable.delegate = dao
				if(params){
					result = callable.call(entity,params)
				}else{
					result = callable.call(entity)
				}

				if(result instanceof Boolean) result = !result
				else {
					result = false
				}
			}
		}
		return result
	}

	// Implemented this for a comman way to do our flush() and clear() methods to get to a clean states
	static void flushAndClear(){
		flushSession()
		clearSession()
	}

	static void flushSession(){
		ctx.sessionFactory.currentSession.flush()
	}
	static void clearSession(){
		ctx.sessionFactory.currentSession.clear()
		DomainClassGrailsPlugin.PROPERTY_INSTANCE_MAP.get().clear()
	}

}

