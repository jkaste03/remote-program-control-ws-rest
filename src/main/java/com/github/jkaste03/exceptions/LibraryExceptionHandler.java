/**
 * 
 */
package com.github.jkaste03.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 
 */
@ControllerAdvice
public class LibraryExceptionHandler {

	/**
	 * Specific error message for user
	 * 
	 * @param ex
	 * @return
	 */
	@ExceptionHandler(value = UnauthorizedOrderActionException.class)
	public ResponseEntity<String> unauthorizedOrder(UnauthorizedOrderActionException ex) {

		return new ResponseEntity<>("An error occured: " + ex.getMessage(), HttpStatus.UNAUTHORIZED);
	}

	/**
	 * All other types of error
	 * 
	 * @param ex
	 * @return
	 */
	@ExceptionHandler(value = Exception.class)
	public ResponseEntity<String> otherErrors(Exception ex) {

		return new ResponseEntity<>("An error occured: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	}

}
