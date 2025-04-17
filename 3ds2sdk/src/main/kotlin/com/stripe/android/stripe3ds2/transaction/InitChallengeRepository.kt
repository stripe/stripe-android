package com.stripe.android.stripe3ds2.transaction

import android.app.Application
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.observability.DefaultErrorReporter
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import com.stripe.android.stripe3ds2.observability.Stripe3ds2ErrorReporterConfig
import com.stripe.android.stripe3ds2.security.DefaultMessageTransformer
import com.stripe.android.stripe3ds2.security.MessageTransformer
import com.stripe.android.stripe3ds2.transactions.ChallengeRequestData
import com.stripe.android.stripe3ds2.transactions.ErrorData
import com.stripe.android.stripe3ds2.views.ChallengeViewArgs
import java.security.cert.X509Certificate
import kotlin.coroutines.CoroutineContext

interface InitChallengeRepository {
    suspend fun startChallenge(
        args: InitChallengeArgs
    ): InitChallengeResult
}

internal class DefaultInitChallengeRepository internal constructor(
    private val sdkTransactionId: SdkTransactionId,
    private val messageVersionRegistry: MessageVersionRegistry,
    private val jwsValidator: JwsValidator,
    private val messageTransformer: MessageTransformer,
    private val acsDataParser: AcsDataParser,
    private val challengeRequestResultRepository: ChallengeRequestResultRepository,
    private val errorRequestExecutorFactory: ErrorRequestExecutor.Factory,
    private val uiCustomization: StripeUiCustomization,
    private val errorReporter: ErrorReporter,
    private val logger: Logger
) : InitChallengeRepository {

    /**
     * Make the initial challenge request and return a [InitChallengeResult] representing the
     * result. If successful, will return [InitChallengeResult.Start] to start the challenge UI;
     * otherwise, will return a [InitChallengeResult.End] that indicates the challenge should end.
     */
    override suspend fun startChallenge(
        args: InitChallengeArgs
    ): InitChallengeResult {
        logger.info("Make initial challenge request.")

        return runCatching {

            println("StripeSdk did work?: ${jwsValidator.getPayload(
                requireNotNull("eyJhbGciOiJSUzI1NiIsIng1YyI6WyJNSUlGVERDQ0F6U2dBd0lCQWdJSUR6TUxXbmJZZGNjd0RRWUpLb1pJaHZjTkFRRUxCUUF3ZURFTE1Ba0dBMVVFQmhNQ1ZWTXhFekFSQmdOVkJBb1RDazFoYzNSbGNrTmhjbVF4S0RBbUJnTlZCQXNUSDAxaGMzUmxja05oY21RZ1NXUmxiblJwZEhrZ1EyaGxZMnNnUjJWdUlETXhLakFvQmdOVkJBTVRJVkJTUkNCTllYTjBaWEpEWVhKa0lETkVVeklnU1hOemRXVnlJRk4xWWlCRFFUQWVGdzB5TXpBMU1Ea3hOalV3TURaYUZ3MHlOakExTURneE5qVXdNRFphTUlHb01Rc3dDUVlEVlFRR0V3SlZVekVUTUJFR0ExVUVDQk1LUTJGc2FXWnZjbTVwWVRFVU1CSUdBMVVFQnhNTFUyRnVkR0VnUTJ4aGNtRXhHREFXQmdOVkJBb1REME5CSUZSbFkyaHViMnh2WjJsbGN6RXlNREFHQTFVRUN3d3BRVU5UVFZNdFFVTlRMVll5TVRBdFEwRmZWRVZEU0U1UFRFOUhTVVZUWHkwdE5UWXpNRFV0UTBFeElEQWVCZ05WQkFNVEYzTmxZM1Z5WlhOcFoyNXBibWN1WVhKamIzUXVZMjl0TUlJQklqQU5CZ2txaGtpRzl3MEJBUUVGQUFPQ0FROEFNSUlCQ2dLQ0FRRUE1UkRHYUxLQkdtcnRYSTYyck1xQkt1UHZyTEJhS2RuQkFMQXVDcTN6d0pKQjV1NS9hOHFCM3ZtQ1E1aWtUYmkrc1J4WjdEckpzN29Yc2kwNGczL3drS0ZJTG9OY3JTb3RXNFlBY1VNNVpMbHMyaThRV1R5Yk1qb3paeG5RbWIwZHNvM0xYWUJjMXBENWI0ZDZTaFNsWDdMVnNGVmZNaTVmeG9xb08yQktiMGR1VnRUK1gzVDFyb1UzdGpMOUJyNjB4NXVKTnBiYUI1SE93VVhLY1l4UXUwTG1NRWxCUDRsbFFpZmE0V0NPL1hvSEVodGJyRW51MXlEUm1nRXluRkRXY3htNUZvZzNQckx3d1JKbkNRWHBMbnhlOGxYdU84NmpUblhEOVE0WDdoWEtOZkkzZUlYYk5Id0laZHR3aVFEK2xhZ05pdDVJaEFWREhlSlVpWmlCT3dJREFRQUJvNEdvTUlHbE1BNEdBMVVkRHdFQi93UUVBd0lBZ0RBSkJnTlZIUk1FQWpBQU1CMEdBMVVkRGdRV0JCUVBwNmdvZTg0akhnUVpJV2JEbmU4ZVo0WE41ekFmQmdOVkhTTUVHREFXZ0JSUDk4T2ZZc3BXVkV2ZlNIVExjQnh2UE4wUVJUQklCZ2dyQmdFRkJRY0JBUVE4TURvd09BWUlLd1lCQlFVSE1BR0dMR2gwZEhBNkx5OXZZM053TG5CcmFTNXBaR1Z1ZEdsMGVXTm9aV05yTG0xaGMzUmxjbU5oY21RdVkyOXRNQTBHQ1NxR1NJYjNEUUVCQ3dVQUE0SUNBUUI2dFd5UTEva3FBSFRpTHBMTDZtV3dvNytBbFg1TXExcjFyQTg3Z3NWNC9jOUhkVXNhWjVianJ4cExVcWZEMTloU1FXS0FxS3lBUmx1ZnhZMXhPQkROTVFUTi9oaG56djE0cVB2aXhUckJEa2ZBaW1tUjZxUUJTWEZlMTFuMDhpcnBCa1BraDhYNEFldG5Hd3FTSWRQbWtWT3VZNHcxWkZ4RDg1bGZXamc1UWREVnlhcnN5clM4c3FRaC93bnRkWnJYbDRoeW44ME9EQk11ZG5QbHkremZ2M2RaM1hzZFR5czBoTWs1UUtJTkY1eTd4YVpWMWk4MFRIY1BtRUFTSTF5VnhDQkh0VFUzTDhqaFRKMkU5d09qSFdFbC9oNWxseWZZRTJOYWtkaDQ3cWpFOU16NFFGaUhvWk8xNFFtemRWaWdjOW5nV1lXNHkxOUlJT1RjUmFNZVRINXpMQTVUTm9qTnRqVms5bzlGa1BneUVZSVduei90bjVQL0VpbTdhbkVyenQrR1l2TW9aZzdhL2VaZjV3U0FUWC9kbXJCeml2aXNhUkNoR2hhU1g3a2MzeU5wdXNXNFU4M3Jvd21CNFg4d2p5cXdPWEgxRi8xMWowVWRzaTBpUzdMT29qSk9zQjRvQVBhc3pxQU9FcXFKRVd2b2JBb1ZDbHhHa3A0Vm5HSS8zZVU1TUpMMzQ4azlHT1lkcUgvMlFPMzM3Y2VoYnYwMCtzZnNkY3RJM1JNcjZOREErVTNrbWNEV1I3RjNSc0Y0ak14RlFaY1NaWUlwc3JhVCt6c3B2eWFtZUswZWtmd24yZDlOK2ZDYnR6elA3Z1dJT3dIZ244VitLeTEzd3JZMHdMZ1EwaTZvaHoxQmNvcTltNlJUNkZoVUptaVI4d05NRG9zclNXTlQ4dz09IiwiTUlJR25UQ0NCSVdnQXdJQkFnSVFDV1hBZ2lXL3hRdTZXUUdpMGxIeEtUQU5CZ2txaGtpRzl3MEJBUXNGQURCOE1Rc3dDUVlEVlFRR0V3SlZVekVUTUJFR0ExVUVDaE1LVFdGemRHVnlRMkZ5WkRFb01DWUdBMVVFQ3hNZlRXRnpkR1Z5UTJGeVpDQkpaR1Z1ZEdsMGVTQkRhR1ZqYXlCSFpXNGdNekV1TUN3R0ExVUVBeE1sVUZKRUlFMWhjM1JsY2tOaGNtUWdTV1JsYm5ScGRIa2dRMmhsWTJzZ1VtOXZkQ0JEUVRBZUZ3MHhOekF4TURFd05UQXdNREJhRncweU5qQTNNVFV3TnpBd01EQmFNSGd4Q3pBSkJnTlZCQVlUQWxWVE1STXdFUVlEVlFRS0V3cE5ZWE4wWlhKRFlYSmtNU2d3SmdZRFZRUUxFeDlOWVhOMFpYSkRZWEprSUVsa1pXNTBhWFI1SUVOb1pXTnJJRWRsYmlBek1Tb3dLQVlEVlFRREV5RlFVa1FnVFdGemRHVnlRMkZ5WkNBelJGTXlJRWx6YzNWbGNpQlRkV0lnUTBFd2dnSWlNQTBHQ1NxR1NJYjNEUUVCQVFVQUE0SUNEd0F3Z2dJS0FvSUNBUUNiRFd6NTVoOFloVXRIanM4M3lUcG9GTmJVZ0l2blBjSlRTRVVwaWlDTGVkaWNoNjFEcFFYZjhuQklkVUZ2K3pzN2pFS1MrbE5jWng3NWlVd0dKMm1Sb1JOaWtKajduS1d3SVBHU3Joc0plT0dSbndrZzZraDlQRUVOUmdVeWw2RE1Vc2tQWWhaOStLTmV1SHJ0M0hlK3NmQmJsSy9SUTNKUnN5TlY5Mll0TVBCQWsyTmRkQnZtaDkvRlNueXUxM3dTaExVYXR4eXNaNjk4T0laMmJwWHNEMkZPM01qNDlSWHBYU3lNT3pSbldNQjFPSkNjcVoxblBKaEczdnBIM3ZhYlRlaFgyY0dMM3hrYWE2SVRXNm1rcm5lS2puS29TM00zM2JSUmUrT2dBbmx1b2NZNitxSmhxTWtDTXBGTFFMR1VjbmtYaXJGQ0txTUw1T0FKV1hRUWdSS0ZZMk9yREF1Z01QdnNtNHlOVmhFQm9jSHd3NnVjNjZKdnJFN1RCa3k2SEE4dklKWDNzVlVQbDJILzVIdlVrVmhrRVJ3bWhTNktqMnhOVmR3MjJRVXpYY2E1d3B4aHU1cXprWUt1RThmQlNUT2g1V0srQUVmc2Jxa1oxMDdVZzYrcmtMWklDcCsvaGJUbkltMG56b0pqc05OdldiSkh4alAwRzJEMzYyUWk2b08zVUtrZDlvc3cyOUNkSFNia2phWXBxQkVrbUMrYzJiQ3FyQVByZUxCV2Q0MWlpcVJlS0ptTWg4eVMvMzJIOXhQQkhpZFBwMGxmNzJDUjJaL0M5dEpQTWdZVWZFVWtiUnVUNVNRKzMwK1ROSVlsWmd5THg1dUxYcnpYS3p4c0ErYWZTbmp4U2dtWFV3Z2VxNkZIU0w2OVF1SGlGUXJLZ0dESURYNnByd0lEQVFBQm80SUJIVENDQVJrd1NBWUlLd1lCQlFVSEFRRUVQREE2TURnR0NDc0dBUVVGQnpBQmhpeG9kSFJ3T2k4dmIyTnpjQzV3YTJrdWFXUmxiblJwZEhsamFHVmpheTV0WVhOMFpYSmpZWEprTG1OdmJUQnBCZ05WSFI4RVlqQmdNRjZnWEtCYWhsaG9kSFJ3T2k4dlkzSnNMbkJyYVM1cFpHVnVkR2wwZVdOb1pXTnJMbTFoYzNSbGNtTmhjbVF1WTI5dEwyUTBZVFUxTVdGaE9USmtZVEV4T0dJeE5HTTBabVl4TVRkaFpURXhaR1V4TXpGbU9UUmhZakV1WTNKc01BNEdBMVVkRHdFQi93UUVBd0lCaGpBU0JnTlZIUk1CQWY4RUNEQUdBUUgvQWdFQU1CMEdBMVVkRGdRV0JCUlA5OE9mWXNwV1ZFdmZTSFRMY0J4dlBOMFFSVEFmQmdOVkhTTUVHREFXZ0JUVXBWR3FrdG9SaXhURS94RjY0UjNoTWZsS3NUQU5CZ2txaGtpRzl3MEJBUXNGQUFPQ0FnRUFtMDJCdjZpSS9aa3ZzcjZkQUcvZ2lKQ08rcGV2REg4MFdHaGFndnZ5ZUljakl3d2ZzU0lVYjl3MzVYUlJoQ09reFNuUDdsdVltbk9jb2p2U0RFbmVVVmJTc014OS96cS9SMUJsL0Z3QUlrc3pKQWNneE5oMXAvNEg5akpUVkZFMFQxSmw2QzFZMG9GK1gwZmhVeFFwSDFGWUJYQTRXWlQrN1JERy8zM09EOG5kQlRPd291ZWgvTjRFd0ZabDhSNGsxRnJkZWFSbm11eFNvSUlrYnY2SWRkVk0zT2dUTU5Ycmk0Y05QM0gyanBpMTRCZ2hiV3kzVjI4MjRob0RXd2ZWbzdTTWJQMU14b1hvbzVIV3BFRW91a3RRWFhqTkRZWXRkRllRV0RtbHI2RmhlQUgxUW51enR3eGQ4NzgvQzlEdmN1c3VWOEhQdEFGUVZWanNYbzNnSWN0bjVOREErY3pYdndkWWNoK1NxWmJtNVl2Yk1UOVJKWXpuWkFzUzNmRC9zZFp1VGdqMzBtWTEyZitKdWdoTEtheXpldHg1T09vR29UcVFQRm5rMEo4cis0aWN3UU5LTnhOdkhUTmhEVmtQVXNvZS9aMDc2ZWR2MnJDWDR0eVZhWHJWWjUwSzA0ZlY3azBIZ1ZJbFF6WUVZZVE4NGxuSWJ1TWVNWjcwYmcrRWFOTk5adW9kZkh1dlVRa2UyL0dRRzNENzVWWU1jd3FkQUFHSDZKdzVPZG5EN000Vy9LaEVuSXpqOWNqb1pnTnRZYktXRk5vdWF0NGdQbjNiR2xJbmZacHJrRzJDRXBJbElacDJxVk1sVDg1ZmFnREZqb2k4SHNzZlFtYm00R0JPc0NPUDV0T3RvWVVqaHdlYTQyOTdDT24ybnk2d0MyN1ZEbGM2T21uRDJUOD0iLCJNSUlGeHpDQ0E2K2dBd0lCQWdJUUZzanlJdXFodzgwd05NalhVNDdsZmpBTkJna3Foa2lHOXcwQkFRc0ZBREI4TVFzd0NRWURWUVFHRXdKVlV6RVRNQkVHQTFVRUNoTUtUV0Z6ZEdWeVEyRnlaREVvTUNZR0ExVUVDeE1mVFdGemRHVnlRMkZ5WkNCSlpHVnVkR2wwZVNCRGFHVmpheUJIWlc0Z016RXVNQ3dHQTFVRUF4TWxVRkpFSUUxaGMzUmxja05oY21RZ1NXUmxiblJwZEhrZ1EyaGxZMnNnVW05dmRDQkRRVEFlRncweE5qQTNNVFF3TnpJME1EQmFGdzB6TURBM01UVXdPREV3TURCYU1Id3hDekFKQmdOVkJBWVRBbFZUTVJNd0VRWURWUVFLRXdwTllYTjBaWEpEWVhKa01TZ3dKZ1lEVlFRTEV4OU5ZWE4wWlhKRFlYSmtJRWxrWlc1MGFYUjVJRU5vWldOcklFZGxiaUF6TVM0d0xBWURWUVFERXlWUVVrUWdUV0Z6ZEdWeVEyRnlaQ0JKWkdWdWRHbDBlU0JEYUdWamF5QlNiMjkwSUVOQk1JSUNJakFOQmdrcWhraUc5dzBCQVFFRkFBT0NBZzhBTUlJQ0NnS0NBZ0VBeFpGM25DRWlUOFhGRmFxKzNCUFQwY01EbFdFNzZJQnNkeDI3dzNoTHh3VkxvZzQyVVRhc0lnem15c1RLcEJjMTdIRVp5TkFxazlHckNIbzBPeWs0Slp1WEhvVzgwZ29aYVIyc01ubjQ5eXR0N2FHc0UxUHNmVnVwOGdxQW9yZm0zSUZhYjIvQ25pSkpOWGFXUGduOTQrVS9uc29hcVRRNmorNkpCb0l3bkZrbGhiWEhmS3JxbGtVWkpDWWFXYlpSaVE3bmtBTllZTTJUZDNOODdGbVJhbm1EWGo1Qkc2bGM5bzFjbFRDN1V2UlFtTklMOU9kRERaOHFscVkyRmkwZXp0Qm51bzJEVVM1dEdkVnk4U2dxUE0zRTEyZnRrNEVkbEt5cldtQnFGY1l3R3g0QWNTSjg4TzNyUW1SQk14dGswcjV2aGdyNmhEQ0dxN0ZISy9oUUZQOUxoVU85MXF4V0V0TW43NlNhN0RQQ0xhcyt0Zk5SVndHMTJGQnVFWkZoZFMvcUtNZElZVUU1UTZ1d0dURXZUemcya21nSlQzc05hNmRiaGxZblluOWlJalRoMGRQR2dpWGFwMUJoaThCOWFhUEZjSEVIU3FXOG5aVUlOY3J3ZjVBVWkrN0QrcS9BRzVJdGlCdFFUQ2FhRm03NGd2NTF5dXR6d2dLbkg5USt4M210dUsvdXdsTENzbGo5RGVYZ096TVdGeEZndXV3TEdYMzlrdERuZXR4TnczUExhYmpIa0RsR0RJZngwTUNRYWtNNzRzVGN1VzhJQ2lIdk5BN2Z4WENuYnRqc3k3YXQveVhZd0FkK0lEUzUxTUEvZzNPWVZONE0rMHBHODQzUmU2WjUzb09EcDBZbXVneDBGTk8xTnhUM0hPMWhkN2RYeWpBVi90Ti9HR2NDQXdFQUFhTkZNRU13RGdZRFZSMFBBUUgvQkFRREFnR0dNQklHQTFVZEV3RUIvd1FJTUFZQkFmOENBUUV3SFFZRFZSME9CQllFRk5TbFVhcVMyaEdMRk1UL0VYcmhIZUV4K1VxeE1BMEdDU3FHU0liM0RRRUJDd1VBQTRJQ0FRQkxxSVlvcnJ0Vno1NkY2V09vTFg5Q2NSalNGaW03Z084NzNhM3A3KzYySTZqb1hNc01yMG5kOW5SUGNFd2R1RWxvWlh3RmdFclZVUVdhVVpXTnB1ZTBtR3ZVN0JVQWdWOVR1MEoweUErOXNyaXpWb012eCtvNHpUSjNWdTVwNWFUZjFhWW9IMXhZVm81b29GZ2wvaEkvRVhEMmxvL3hPVWZQS1hCWTd0d2ZpcU96aVFtVEdCdXFQUnE4aDNkUVJsWFl4WC9yekdmODBTZWNJVDZ3bzlLYXZEa2pPbUpXR3p6SHNuNlJ5bzZNRUNsTWFQbjB0ZTg3dWtOTjc0MEFkUGhUdk5lWmRXbHd5cVdBSnBzdjI0Y2FFY2tqU3BncG9JWk9qYzdQQWNFVlFPV0ZTeFVlc01rNEp6NWJWWmEvQUJqemNwK3JzcTFRTFNKNXF1cUh3V0ZUZXdDaHdwdzVncHcrRTVTcEtZNkZJSFBsVGRsK3FIVGh2Tjhsc0tOQVFnMHFUZEViSUZaQ1VRQzBDbDNUaTNxL2NYdjh0Z3VMSk5XdmRHekI2MDBZMzJRSGNsTXBleWFiVDQvUWVPZXNxcHg2RGE3MEoyS3ZMVDFqNkNoMkJzS1N6ZVZMYWhyam5vUHJkZ2lJWVlCT2dlQTNUOFNFMXBnYWd0NTZSN25Ja1JRYnRlc29SS2krTmZDN3BQYi9HMVZVc2ovY1JFQUhIMWkxVUthMGFDc0lpQU5mRWRRTjVPazZ3dEZKSmhwM2FwQXZuVmtyWkRmT0c1d2U5Yll6dkdvSTdTVW5sZVVSQkorTjNpaGpBUmZMNGhEZWVSSGhZeUxrTTNrRXlFa3JKQkw1cjBHRGppY3hNK2FGY1IyZkNCQWt2M2dyVDVrejRrTGN2c21IWCs5REJ3PT0iXX0.eyJhY3NFcGhlbVB1YktleSI6eyJjcnYiOiJQLTI1NiIsImt0eSI6IkVDIiwieCI6ImdZWkduRUlmY1dlaU4wS0k5TllKQVNOUVI0WWdHeFI2LXZ2NmxneGlkV2siLCJ5IjoiNGZrZkNmYWVCelVOa3c0VzhBcExSZVlnS1hfX0diYkFjV2xXcmVKRVNzTSJ9LCJhY3NVUkwiOiJodHRwczovL3NlY3VyZTcuYXJjb3QuY29tL2Fjcy9hcGkvdGRzMi90eG4vYXBwL3YxL2NyZXEiLCJzZGtFcGhlbVB1YktleSI6eyJjcnYiOiJQLTI1NiIsImt0eSI6IkVDIiwieCI6ImFXRGI2bS1ndmVWQlJobEhtSzhVcDN5SFB1eV9LR3NfbnBVMWNEQlZ1WUkiLCJ5IjoiMWpVQ2JJSkJyNDdKS2FkZEktN05veFdWT0dna1FfcGxlMmRGRThIV3Q4ayJ9fQ.d0BzkTYA43BNvXYtil7o-kX9yZI7wNhcubABGASOCdopGk0xg_Y03ZmSwTZm7OKQIdUGvlDVy2mYQATVh-ihuYKU4WMZt8etiML9i3_qPgDEh4C4sow4FY1FBVlSNBuVUM1W-8DyLDEC8CAnyUGtyPj2UlfG7ome4_oFMq7dEO7J0ZpBS_tDcusE8sP1tIszhVogPvGnpnGiB-ReTSqY5gYaVFVZCdlnb-3fjpD5WdtE6tu89rWxfdoaV69InLHzt_eL6s4QLF39yOh8RuK288wERu-vdzPzsyAxxdvly9w2HJa6NedXn6wiNOR_ArC8JfCCFm3-o0-ktHOrxKre9w")
            )}")
            // will throw exception if acsSignedContent fails verification
            val (acsUrl, acsEphemPubKey) = acsDataParser.parse(
                jwsValidator.getPayload(
                    requireNotNull(args.challengeParameters.acsSignedContent)
                )
            )



            val creqData = createCreqData(sdkTransactionId, args.challengeParameters)

            val errorRequestExecutor = errorRequestExecutorFactory.create(acsUrl, errorReporter)

            val creqExecutorConfig = ChallengeRequestExecutor.Config(
                messageTransformer,
                args.sdkReferenceNumber,
                creqData,
                acsUrl,
                ChallengeRequestExecutor.Config.Keys(
                    args.sdkKeyPair.private.encoded,
                    acsEphemPubKey.encoded
                )
            )

            val challengeRequestResult = challengeRequestResultRepository.get(
                creqExecutorConfig,
                creqData
            )

            when (challengeRequestResult) {
                is ChallengeRequestResult.Success -> {
                    InitChallengeResult.Start(
                        ChallengeViewArgs(
                            challengeRequestResult.cresData,
                            challengeRequestResult.creqData,
                            uiCustomization,
                            creqExecutorConfig,
                            StripeChallengeRequestExecutor.Factory(creqExecutorConfig),
                            args.timeoutMins,
                            args.intentData
                        )
                    )
                }
                is ChallengeRequestResult.ProtocolError -> {
                    if (challengeRequestResult.data.errorComponent == ErrorData.ErrorComponent.ThreeDsSdk) {
                        errorRequestExecutor.executeAsync(challengeRequestResult.data)
                    }

                    InitChallengeResult.End(
                        ChallengeResult.ProtocolError(
                            challengeRequestResult.data,
                            null,
                            args.intentData
                        )
                    )
                }
                is ChallengeRequestResult.Timeout -> {
                    errorRequestExecutor.executeAsync(challengeRequestResult.data)

                    InitChallengeResult.End(
                        ChallengeResult.Timeout(
                            null,
                            null,
                            args.intentData
                        )
                    )
                }
                is ChallengeRequestResult.RuntimeError -> {
                    InitChallengeResult.End(
                        ChallengeResult.RuntimeError(
                            challengeRequestResult.throwable,
                            null,
                            args.intentData
                        )
                    )
                }
            }
        }.getOrElse {
            errorReporter.reportError(it)
            logger.error("Exception during initial challenge request.", it)

            InitChallengeResult.End(
                ChallengeResult.RuntimeError(
                    it,
                    null,
                    args.intentData
                )
            )
        }
    }

    private fun createCreqData(
        sdkTransactionId: SdkTransactionId,
        challengeParameters: ChallengeParameters
    ) = ChallengeRequestData(
        acsTransId = requireNotNull(challengeParameters.acsTransactionId),
        threeDsServerTransId = requireNotNull(challengeParameters.threeDsServerTransactionId),
        sdkTransId = sdkTransactionId,
        messageVersion = messageVersionRegistry.current,
        threeDSRequestorAppURL = challengeParameters.threeDSRequestorAppURL
    )
}

class InitChallengeRepositoryFactory(
    private val application: Application,
    private val isLiveMode: Boolean,
    private val sdkTransactionId: SdkTransactionId,
    private val uiCustomization: StripeUiCustomization,
    private val rootCerts: List<X509Certificate>,
    private val enableLogging: Boolean,
    private val workContext: CoroutineContext,
) {
    fun create(): InitChallengeRepository {
        val logger = Logger.get(enableLogging)
        val errorReporter = DefaultErrorReporter(
            application,
            Stripe3ds2ErrorReporterConfig(sdkTransactionId),
            workContext,
            logger
        )
        return DefaultInitChallengeRepository(
            sdkTransactionId,
            MessageVersionRegistry(),
            DefaultJwsValidator(
                isLiveMode,
                rootCerts,
                errorReporter
            ),
            DefaultMessageTransformer(isLiveMode),
            DefaultAcsDataParser(errorReporter),
            DefaultChallengeRequestResultRepository(errorReporter, workContext),
            StripeErrorRequestExecutor.Factory(workContext),
            uiCustomization,
            errorReporter,
            logger
        )
    }
}
