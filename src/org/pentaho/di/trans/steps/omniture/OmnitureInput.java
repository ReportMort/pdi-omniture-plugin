/*! ******************************************************************************
*
* Pentaho Data Integration
*
* Copyright (C) 2002-2013 by Pentaho : http://www.pentaho.com
*
*******************************************************************************
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
******************************************************************************/

package org.pentaho.di.trans.steps.omniture;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.omniture.OmnitureInputData;
import org.pentaho.di.trans.steps.omniture.OmnitureInputMeta;

import com.adobe.analytics.client.*;
import com.adobe.analytics.client.domain.*;
import com.adobe.analytics.client.methods.*;

/**
 * This class is part of the demo step plug-in implementation.
 * It demonstrates the basics of developing a plug-in step for PDI. 
 * 
 * The demo step adds a new string field to the row stream and sets its
 * value to "Hello World!". The user may select the name of the new field.
 *   
 * This class is the implementation of StepInterface.
 * Classes implementing this interface need to:
 * 
 * - initialize the step
 * - execute the row processing logic
 * - dispose of the step 
 * 
 * Please do not create any local fields in a StepInterface class. Store any
 * information related to the processing logic in the supplied step data interface
 * instead.  
 * 
 */

public class OmnitureInput extends BaseStep implements StepInterface {
	
	private static Class<?> PKG = OmnitureInputMeta.class; 
    private OmnitureInputMeta meta;
    private OmnitureInputData data;

	/**
	 * The constructor should simply pass on its arguments to the parent class.
	 * 
	 * @param s 				step description
	 * @param stepDataInterface	step data class
	 * @param c					step copy
	 * @param t					transformation description
	 * @param dis				transformation executing
	 */
	public OmnitureInput(StepMeta s, StepDataInterface stepDataInterface, 
			int c, TransMeta t, Trans dis) {
		super(s, stepDataInterface, c, t, dis);
	}
	
	/**
	 * This method is called by PDI during transformation startup. 
	 * 
	 * It should initialize required for step execution. 
	 * 
	 * The meta and data implementations passed in can safely be cast
	 * to the step's respective implementations. 
	 * 
	 * It is mandatory that super.init() is called to ensure correct behavior.
	 * 
	 * Typical tasks executed here are establishing the connection to a database,
	 * as wall as obtaining resources, like file handles.
	 * 
	 * @param smi 	step meta interface implementation, containing the step settings
	 * @param sdi	step data interface implementation, used to store runtime information
	 * 
	 * @return true if initialization completed successfully, false if there was an error preventing the step from working. 
	 *  
	 */
	  /**
	   * Build an empty row based on the meta-data.
	   *
	   * @return
	   */
	  public boolean init( StepMetaInterface smi, StepDataInterface sdi ) {
		
	    meta = (OmnitureInputMeta) smi;
	    data = (OmnitureInputData) sdi;

	    if ( super.init( smi, sdi ) ) {
	      // get total fields in the grid
	      data.nrfields = meta.getInputFields().length;
	      // Check if field list is filled
	      if ( data.nrfields == 0 ) {
	        log.logError( BaseMessages.getString( PKG, "OmnitureInputDialog.FieldsMissing.DialogMessage" ) );
	        return false;
	      }
	      
	      // check username
	      String realUser = environmentSubstitute( meta.getUserName() );
	      if ( Const.isEmpty( realUser ) ) {
	        log.logError( BaseMessages.getString( PKG, "OmnitureInput.UsernameMissing.Error" ) );
	        return false;
	      }
	      // check secret
	      String realSecret = environmentSubstitute( meta.getSecret() );
	      if ( Const.isEmpty( realSecret ) ) {
	        log.logError( BaseMessages.getString( PKG, "OmnitureInput.SecretMissing.Error" ) );
	        return false;
	      }
	      // check report suite id
	      String realReportSuiteId = environmentSubstitute( meta.getReportSuiteId() );
	      if ( Const.isEmpty( realReportSuiteId ) ) {
	        log.logError( BaseMessages.getString( PKG, "OmnitureInput.ReportSuiteIdMissing.Error" ) );
	        return false;
	      }
	      try{
	      data.client = new AnalyticsClientBuilder()
	    		  .setEndpoint("api2.omniture.com")
	    		  .authenticateWithSecret(realUser, realSecret)
	    		  .build();
	      
	        return true;
	      }  catch ( Exception e ) {
              log.logError( BaseMessages.getString( PKG, "OmnitureInput.Log.ErrorOccurredDuringStepInitialize" ), e );
          }
	      return true;
	    }
	    return false;
	  }	

	/**
	 * Once the transformation starts executing, the processRow() method is called repeatedly
	 * by PDI for as long as it returns true. To indicate that a step has finished processing rows
	 * this method must call setOutputDone() and return false;
	 * 
	 * Steps which process incoming rows typically call getRow() to read a single row from the
	 * input stream, change or add row content, call putRow() to pass the changed row on 
	 * and return true. If getRow() returns null, no more rows are expected to come in, 
	 * and the processRow() implementation calls setOutputDone() and returns false to
	 * indicate that it is done too.
	 * 
	 * Steps which generate rows typically construct a new row Object[] using a call to
	 * RowDataUtil.allocateRowData(numberOfFields), add row content, and call putRow() to
	 * pass the new row on. Above process may happen in a loop to generate multiple rows,
	 * at the end of which processRow() would call setOutputDone() and return false;
	 * 
	 * @param smi the step meta interface containing the step settings
	 * @param sdi the step data interface that should be used to store
	 * 
	 * @return true to indicate that the function should be called again, false if the step is done
	 */
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {

		// safely cast the step settings (meta) and runtime info (data) to specific implementations 
		OmnitureInputMeta meta = (OmnitureInputMeta) smi;
		OmnitureInputData data = (OmnitureInputData) sdi;

		// get incoming row, getRow() potentially blocks waiting for more rows, returns null if no more rows expected
		Object[] r = getRow(); 
		
		// if no more rows are expected, indicate step is finished and processRow() should not be called again
		if (r == null){
			setOutputDone();
			return false;
		}

		// the "first" flag is inherited from the base step implementation
		// it is used to guard some processing tasks, like figuring out field indexes
		// in the row structure that only need to be done once
		Report report = new Report();
		List<Record> records = new ArrayList<>();
		
		if (first) {
		  first = false;
	      // Create the output row meta-data
	      data.outputRowMeta = new RowMeta();
		  // clone the input row structure and place it in our data object
		  data.outputRowMeta = (RowMetaInterface) getInputRowMeta().clone();
		  // use meta.getFields() to change it, so it reflects the output row structure 
		  meta.getFields(data.outputRowMeta, getStepname(), null, null, this, repository, metaStore );
		  
	      // Let's query Omniture
			AnalyticsClient client = new AnalyticsClientBuilder()
				    .setEndpoint("api2.omniture.com")
				    .authenticateWithSecret("kjahsdfkjshdflkajsdh", 
				    		"dfaslfdkjasdlfkajf")
				    .build();
			
			ReportDescription desc = new ReportDescription();
			desc.setReportSuiteID("sdf");
			desc.setDateFrom("2015-01-01"); // YYYY-MM-DD
			desc.setDateTo("2015-01-30");
			desc.setDateGranularity(ReportDescriptionDateGranularity.WEEK);
			desc.setMetricIds("pageviews");
			desc.setElementIds("eVar2");	
			ReportMethods reportMethods = new ReportMethods(client);
			int reportId = 0;
			ReportResponse response = null;
			try {
				reportId = reportMethods.queue(desc);
				while (response == null) {
				    try {
				        response = reportMethods.get(reportId);
				    } catch (ApiException e) {
				        if ("report_not_ready".equals(e.getError())) {
				            System.err.println("Report not ready yet.");
				            try {
								Thread.sleep(3000);
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
				            continue;
				        }
				        throw e;
				    }
				}
			} catch (IOException i) {
				System.err.println("Report queuing error.");
			}
			
		    /* Get the report */
			report = response.getReport();

			records = flattenReportData(report.getData(), 
					new Record(report.getMetrics().size() + 1));
			Iterable<String> headers = getHeaders(report);
			logBasic(headers.toString());
			// need to figure out how to put on the stream
			for (Record record : records) {
				if (!record.isComplete()) {
					continue;
				}
				logBasic(record.toString());
			}
		}

		// safely add the string "Hello World!" at the end of the output row
		// the row array will be resized if necessary 
		Object[] outputRow = RowDataUtil.addValueData(r, data.outputRowMeta.size() - 1, "H!");

		// put the row to the output row stream
		putRow(data.outputRowMeta, outputRow); 
				
		/*
	    Object[] outputRowData = null;

	    try {
	      // get one row ...
	      outputRowData = getOneRow();

	      if ( report.getData().size() < 1 ) {
	        setOutputDone();
	        return false;
	      }

	      putRow( data.outputRowMeta, outputRowData ); // copy row to output rowset(s);

	      if ( checkFeedback( getLinesInput() ) ) {
	        if ( log.isDetailed() ) {
	          logDetailed("detaillog");
	        }
	      }
	      return true;
	    } catch ( KettleException e ) {
	      boolean sendToErrorRow = false;
	      String errorMessage = null;
	      if ( getStepMeta().isDoingErrorHandling() ) {
	        sendToErrorRow = true;
	        errorMessage = e.toString();
	      } else {
	        setErrors( 1 );
	        stopAll();
	        setOutputDone(); // signal end to receiver(s)
	        return false;
	      }
	      if ( sendToErrorRow ) {
	        // Simply add this row to the error row
	        putError( getInputRowMeta(), outputRowData, 1, errorMessage, null, "OmnitureInput001" );
	      }
	    }
	    
	    */
	    
	    return true;
	}

	/**
	 * This method is called by PDI once the step is done processing. 
	 * 
	 * The dispose() method is the counterpart to init() and should release any resources
	 * acquired for step execution like file handles or database connections.
	 * 
	 * The meta and data implementations passed in can safely be cast
	 * to the step's respective implementations. 
	 * 
	 * It is mandatory that super.dispose() is called to ensure correct behavior.
	 * 
	 * @param smi 	step meta interface implementation, containing the step settings
	 * @param sdi	step data interface implementation, used to store runtime information
	 */
	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {

		// Casting to step-specific implementation classes is safe
		OmnitureInputMeta meta = (OmnitureInputMeta) smi;
		OmnitureInputData data = (OmnitureInputData) sdi;
		
		super.dispose(meta, data);
	}
	
	private static List<Record> flattenReportData(List<ReportData> dataList, Record partialRecord) {
		final List<Record> records = new ArrayList<>();
		for (final ReportData data : dataList) {
			final Record record = partialRecord.clone();
			record.addElements(data);
			if (data.getBreakdown() == null) {
				record.addMetrics(data);
				records.add(record);
			} else {
				records.addAll(flattenReportData(data.getBreakdown(), record));
			}
		}
		return records;
	}
	
	private static Iterable<String> getHeaders(Report report) {
		final List<String> headers = new ArrayList<>();
		headers.add("name");

		final ReportData data = report.getData().get(0);
		if (data.getYear() != null) {
			headers.add("year");
		}
		if (data.getMonth() != null) {
			headers.add("month");
		}
		if (data.getDay() != null) {
			headers.add("day");
		}
		if (data.getHour() != null) {
			headers.add("hour");
		}
		if (data.getMinute() != null) {
			headers.add("minute");
		}
		for (final ReportElement e : report.getElements()) {
			headers.add(e.getId());
		}
		for (final ReportMetric m : report.getMetrics()) {
			headers.add(m.getId());
		}
		return headers;
	}

	/*
  private Object[] buildEmptyRow() {
	    Object[] rowData = RowDataUtil.allocateRowData( records.outputRowMeta.size() );
	    return rowData;
	  }
  
  private Object[] getOneRow() throws KettleException {
	    if ( data.limitReached || data.rownr >= data.recordcount ) {
	      return null;
	    }

	    // Build an empty row based on the meta-data
	    Object[] outputRowData = buildEmptyRow();

	    try {

	      // check for limit rows
	      if ( data.limit > 0 && data.rownr >= data.limit ) {
	        // User specified limit and we reached it
	        // We end here
	        data.limitReached = true;
	        return null;
	      } else {
	        if ( data.rownr >= data.nrRecords || data.finishedRecord ) {
	          if ( meta.getRecordsFilter() != SalesforceConnectionUtils.RECORDS_FILTER_UPDATED ) {
	            // We retrieved all records available here
	            // maybe we need to query more again ...
	            if ( log.isDetailed() ) {
	              logDetailed( BaseMessages.getString( PKG, "OmnitureInput.Log.NeedQueryMore", "" + data.rownr ) );
	            }

	            if ( data.connection.queryMore() ) {
	              // We returned more result (query is not done yet)
	              int nr = data.connection.getRecordsCount();
	              data.nrRecords += nr;
	              if ( log.isDetailed() ) {
	                logDetailed( BaseMessages.getString( PKG, "OmnitureInput.Log.QueryMoreRetrieved", "" + nr ) );
	              }

	              // We need here to initialize recordIndex
	              data.recordIndex = 0;

	              data.finishedRecord = false;
	            } else {
	              // Query is done .. we finished !
	              return null;
	            }
	          }
	        }
	      }

	      // Return a record
	      SalesforceRecordValue srvalue = data.connection.getRecord( data.recordIndex );
	      data.finishedRecord = srvalue.isAllRecordsProcessed();

	      if ( meta.getRecordsFilter() == SalesforceConnectionUtils.RECORDS_FILTER_DELETED ) {
	        if ( srvalue.isRecordIndexChanges() ) {
	          // We have moved forward...
	          data.recordIndex = srvalue.getRecordIndex();
	        }
	        if ( data.finishedRecord && srvalue.getRecordValue() == null ) {
	          // We processed all records
	          return null;
	        }
	      }
	      for ( int i = 0; i < data.nrfields; i++ ) {
	        String value =
	          data.connection.getRecordValue( srvalue.getRecordValue(), meta.getInputFields()[i].getField() );

	        // DO Trimming!
	        switch ( meta.getInputFields()[i].getTrimType() ) {
	          case OmnitureInputField.TYPE_TRIM_LEFT:
	            value = Const.ltrim( value );
	            break;
	          case OmnitureInputField.TYPE_TRIM_RIGHT:
	            value = Const.rtrim( value );
	            break;
	          case OmnitureInputField.TYPE_TRIM_BOTH:
	            value = Const.trim( value );
	            break;
	          default:
	            break;
	        }

	        // DO CONVERSIONS...
	        //
	        ValueMetaInterface targetValueMeta = data.outputRowMeta.getValueMeta( i );
	        ValueMetaInterface sourceValueMeta = data.convertRowMeta.getValueMeta( i );
	        outputRowData[i] = targetValueMeta.convertData( sourceValueMeta, value );

	        // Do we need to repeat this field if it is null?
	        if ( meta.getInputFields()[i].isRepeated() ) {
	          if ( data.previousRow != null && Const.isEmpty( value ) ) {
	            outputRowData[i] = data.previousRow[i];
	          }
	        }

	      } // End of loop over fields...

	      int rowIndex = data.nrfields;

	      // See if we need to add the url to the row...
	      if ( meta.includeTargetURL() && !Const.isEmpty( meta.getTargetURLField() ) ) {
	        outputRowData[rowIndex++] = data.connection.getURL();
	      }

	      // See if we need to add the module to the row...
	      if ( meta.includeModule() && !Const.isEmpty( meta.getModuleField() ) ) {
	        outputRowData[rowIndex++] = data.connection.getModule();
	      }

	      // See if we need to add the generated SQL to the row...
	      if ( meta.includeSQL() && !Const.isEmpty( meta.getSQLField() ) ) {
	        outputRowData[rowIndex++] = data.connection.getSQL();
	      }

	      // See if we need to add the server timestamp to the row...
	      if ( meta.includeTimestamp() && !Const.isEmpty( meta.getTimestampField() ) ) {
	        outputRowData[rowIndex++] = data.connection.getServerTimestamp();
	      }

	      // See if we need to add the row number to the row...
	      if ( meta.includeRowNumber() && !Const.isEmpty( meta.getRowNumberField() ) ) {
	        outputRowData[rowIndex++] = new Long( data.rownr );
	      }

	      if ( meta.includeDeletionDate() && !Const.isEmpty( meta.getDeletionDateField() ) ) {
	        outputRowData[rowIndex++] = srvalue.getDeletionDate();
	      }

	      RowMetaInterface irow = getInputRowMeta();

	      data.previousRow = irow == null ? outputRowData : irow.cloneRow( outputRowData ); // copy it to make
	    } catch ( Exception e ) {
	      throw new KettleException( BaseMessages
	        .getString( PKG, "OmnitureInput.Exception.CanNotReadFromSalesforce" ), e );
	    }

	    return outputRowData;
	  }
	*/
}
