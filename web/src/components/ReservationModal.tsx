import { useEffect, useRef, useState } from 'react'
import { ArrowRight, Check, X } from 'lucide-react'
import type { ReservationSelection } from '../types'
import { useLanguage } from '../context/LanguageContext'

const deviceTypes: ('laptop' | 'smartphone' | 'appliance' | 'etc')[] = ['laptop', 'smartphone', 'appliance', 'etc']

function localDate() {
  const now = new Date()
  return [now.getFullYear(), String(now.getMonth() + 1).padStart(2, '0'), String(now.getDate()).padStart(2, '0')].join('-')
}

export function ReservationModal({ initialSelection, onClose }: { initialSelection: ReservationSelection; onClose: () => void }) {
  const { t } = useLanguage()
  const dialogRef = useRef<HTMLDialogElement>(null)
  const stepTitleRef = useRef<HTMLHeadingElement>(null)
  const dateRef = useRef<HTMLInputElement>(null)
  const [step, setStep] = useState(1)
  const [isComplete, setIsComplete] = useState(false)
  const [form, setForm] = useState({
    device: initialSelection.device,
    symptom: initialSelection.symptom,
    name: '',
    phone: '',
    address: '',
    date: '',
    time: '09:00',
  })

  useEffect(() => {
    const dialog = dialogRef.current!
    const opener = document.activeElement instanceof HTMLElement ? document.activeElement : null
    const overflow = document.body.style.overflow
    dialog.showModal()
    document.body.style.overflow = 'hidden'
    return () => {
      dialog.close()
      document.body.style.overflow = overflow
      if (opener?.isConnected) opener.focus()
    }
  }, [])

  useEffect(() => {
    stepTitleRef.current?.focus()
  }, [step, isComplete])

  return (
    <dialog ref={dialogRef} className="reservation-dialog" aria-labelledby="reservation-title" aria-describedby="reservation-note" onCancel={event => { event.preventDefault(); onClose() }} onKeyDown={event => {
      if (event.key !== 'Tab') return
      const elements = Array.from(event.currentTarget.querySelectorAll<HTMLElement>('button:not(:disabled), input:not(:disabled), textarea:not(:disabled), select:not(:disabled)'))
      const first = elements[0]
      const last = elements.at(-1)
      if (event.shiftKey && (document.activeElement === first || document.activeElement === stepTitleRef.current)) {
        event.preventDefault()
        last?.focus()
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault()
        first?.focus()
      }
    }}>
      <div className="modal-header">
        <div>
          <span className="eyebrow">{t.modal.brand}</span>
          <h2 id="reservation-title">{t.modal.title}</h2>
        </div>
        <button className="icon-button" aria-label={t.modal.closeAria} onClick={onClose}><X size={22} /></button>
      </div>
      <div className="modal-body">
        <p id="reservation-note" className="demo-notice">{t.modal.demoNotice}</p>
        {isComplete ? (
          <div className="reservation-complete">
            <span className="complete-icon"><Check size={28} /></span>
            <h3 ref={stepTitleRef} tabIndex={-1}>{t.modal.complete.title}</h3>
            <p style={{ whiteSpace: 'pre-line' }}>{t.modal.complete.desc}</p>
            <dl className="reservation-summary">
              <div><dt>{t.modal.complete.device}</dt><dd>{(t.modal.devices as Record<string, string>)[form.device] || form.device}</dd></div>
              <div><dt>{t.modal.complete.schedule}</dt><dd>{form.date} {form.time}</dd></div>
            </dl>
            <button className="button primary full-width" onClick={onClose}>{t.modal.complete.btnClose}</button>
          </div>
        ) : (
          <form onSubmit={event => {
            event.preventDefault()
            if (step === 1) { setStep(2); return }
            if (form.date < localDate()) {
              dateRef.current?.setCustomValidity(t.modal.dateLabel)
              dateRef.current?.reportValidity()
              return
            }
            setIsComplete(true)
          }}>
            <ol className="form-steps" aria-label="Steps">
              <li aria-current={step === 1 ? 'step' : undefined}><span>1</span>{t.modal.steps.step1}</li>
              <li aria-current={step === 2 ? 'step' : undefined}><span>2</span>{t.modal.steps.step2} &amp; {t.modal.steps.step3}</li>
            </ol>
            <h3 className="form-title" ref={stepTitleRef} tabIndex={-1}>
              {step === 1 ? `${t.modal.steps.step1}` : `${t.modal.steps.step2}`}
            </h3>
            {step === 1 ? (
              <div className="form-fields">
                <fieldset>
                  <legend>{t.modal.deviceLegend}</legend>
                  <div className="device-options">
                    {deviceTypes.map(devId => (
                      <label key={devId} className="device-option">
                        <input
                          type="radio"
                          name="device"
                          value={devId}
                          checked={form.device === devId}
                          onChange={() => setForm({ ...form, device: devId })}
                        />
                        <span>{t.modal.devices[devId]}</span>
                      </label>
                    ))}
                  </div>
                </fieldset>
                <label className="field">
                  {t.modal.symptomLegend}
                  <textarea
                    required
                    maxLength={2000}
                    rows={4}
                    value={form.symptom}
                    onChange={event => setForm({ ...form, symptom: event.target.value })}
                    placeholder={t.modal.symptomPlaceholder}
                  />
                </label>
                <button className="button primary full-width" type="submit">
                  {t.modal.btnNext} <ArrowRight size={18} />
                </button>
              </div>
            ) : (
              <div className="form-fields">
                <div className="field-row">
                  <label className="field">
                    {t.modal.nameLabel}
                    <input
                      required
                      maxLength={50}
                      value={form.name}
                      onChange={event => setForm({ ...form, name: event.target.value })}
                      placeholder={t.modal.namePlaceholder}
                      autoComplete="off"
                    />
                  </label>
                  <label className="field">
                    {t.modal.phoneLabel}
                    <input
                      required
                      type="tel"
                      pattern="[0-9\-]{9,13}"
                      value={form.phone}
                      onChange={event => setForm({ ...form, phone: event.target.value })}
                      placeholder={t.modal.phonePlaceholder}
                      autoComplete="off"
                    />
                  </label>
                </div>
                <label className="field">
                  {t.modal.addressLabel}
                  <input
                    required
                    maxLength={200}
                    value={form.address}
                    onChange={event => setForm({ ...form, address: event.target.value })}
                    placeholder={t.modal.addressPlaceholder}
                    autoComplete="off"
                  />
                </label>
                <div className="field-row">
                  <label className="field">
                    {t.modal.dateLabel}
                    <input
                      ref={dateRef}
                      required
                      type="date"
                      min={localDate()}
                      value={form.date}
                      onChange={event => { event.target.setCustomValidity(''); setForm({ ...form, date: event.target.value }) }}
                    />
                  </label>
                  <label className="field">
                    {t.modal.timeLabel}
                    <select value={form.time} onChange={event => setForm({ ...form, time: event.target.value })}>
                      {['09:00', '10:00', '11:00', '13:00', '14:00', '15:00', '16:00', '17:00'].map(time => (
                        <option key={time}>{time}</option>
                      ))}
                    </select>
                  </label>
                </div>
                <div className="form-actions">
                  <button type="button" className="button secondary" onClick={() => setStep(1)}>
                    {t.modal.btnPrev}
                  </button>
                  <button type="submit" className="button primary">
                    {t.modal.btnComplete} <Check size={18} />
                  </button>
                </div>
              </div>
            )}
          </form>
        )}
      </div>
    </dialog>
  )
}
