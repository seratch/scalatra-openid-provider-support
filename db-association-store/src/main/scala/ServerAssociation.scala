import scalikejdbc._
import org.joda.time._
import java.util.Date

case class SeverAssociation(
  handle: String,
  _type: String,
  macKey: String,
  datetimeToExpire: DateTime)

object SeverAssociation {

  val tableName = "server_association"

  object columnNames {
    val handle = "handle"
    val _type = "_type"
    val macKey = "mac_key"
    val datetimeToExpire = "datetime_to_expire"
    val all = Seq(handle, _type, macKey, datetimeToExpire)
  }

  val * = {
    import columnNames._
    (rs: WrappedResultSet) => SeverAssociation(
      handle = rs.string(handle),
      _type = rs.string(_type),
      macKey = rs.string(macKey),
      datetimeToExpire = rs.date(datetimeToExpire).toDateTime)
  }

  def findByHandle(handle: String)(implicit session: DBSession = AutoSession): Option[SeverAssociation] = {
    SQL("""SELECT * FROM server_association WHERE handle = {handle}""")
      .bindByName('handle -> handle).map(*).single.apply()
  }

  def create(
    handle: String,
    _type: String,
    mackey: String,
    datetimeToExpire: DateTime)(implicit session: DBSession = AutoSession): SeverAssociation = {
    SQL("""
      INSERT INTO server_association (
        handle,
        _type,
        mac_key,
        datetime_to_expire
      ) VALUES (
        /*'handle*/'abc',
        /*'_type*/'abc',
        /*'macKey*/'abc',
        /*'datetimeToExpire*/'1958-09-06'
      )
      """)
      .bindByName(
        'handle -> handle,
        '_type -> _type,
        'macKey -> mackey,
        'datetimeToExpire -> datetimeToExpire
      ).update.apply()
    SeverAssociation(
      handle = handle,
      _type = _type,
      macKey = mackey,
      datetimeToExpire = datetimeToExpire)
  }

  def deleteExpired()(implicit session: DBSession = AutoSession): Unit = {
    SQL("""DELETE FROM server_association WHERE datetime_to_expire < {date}""")
      .bindByName('date -> new Date).update.apply()
  }

  def deleteByHandle(handle: String)(implicit session: DBSession = AutoSession): Unit = {
    SQL("""DELETE FROM server_association WHERE handle = {handle}""")
      .bindByName('handle -> handle).update.apply()
  }

}

