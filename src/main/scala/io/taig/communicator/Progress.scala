package io.taig.communicator

import scala.math.{ log, pow }

trait Progress {
    def current: Long

    override def toString = Progress.format( current )
}

object Progress {
    private def format( bytes: Long ): String = {
        if ( bytes < 1024 ) {
            bytes + " B"
        }
        else {
            val exp = ( log( bytes ) / Math.log( 1024 ) ).toInt
            "%.2f %s".format( bytes / pow( 1024, exp ), "KMGTPE".charAt( exp - 1 ) + "iB" )
        }
    }

    case class Send( current: Long, total: Long )
            extends Progress {
        def percentage: Float = current / total.toFloat * 100

        override def toString = "%s / %s (%.2f%)".format( super.toString, format( total ), percentage )
    }

    case class Receive( current: Long, total: Option[Long] )
            extends Progress {
        def percentage: Option[Float] = total.map( current / _.toFloat * 100 )

        override def toString = super.toString + ( ( total, percentage ) match {
            case ( Some( total ), Some( percentage ) ) ⇒ " / %s (%.2f%%)".format( format( total ), percentage )
            case _                                     ⇒ ""
        } )
    }
}